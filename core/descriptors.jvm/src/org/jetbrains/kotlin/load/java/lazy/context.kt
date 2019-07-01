/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.lazy

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ClassOrPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.AnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.java.components.JavaPropertyInitializerEvaluator
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.components.SignaturePropagator
import org.jetbrains.kotlin.load.java.lazy.types.JavaTypeResolver
import org.jetbrains.kotlin.load.java.sources.JavaSourceElementFactory
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameterListOwner
import org.jetbrains.kotlin.load.java.typeEnhancement.JavaTypeQualifiers
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifierWithMigrationStatus
import org.jetbrains.kotlin.load.java.typeEnhancement.SignatureEnhancement
import org.jetbrains.kotlin.load.kotlin.DeserializedDescriptorResolver
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.storage.StorageManager
import java.util.*

class JavaResolverComponents(
    val storageManager: StorageManager,
    val finder: JavaClassFinder,
    val kotlinClassFinder: KotlinClassFinder,
    val deserializedDescriptorResolver: DeserializedDescriptorResolver,
    val signaturePropagator: SignaturePropagator,
    val errorReporter: ErrorReporter,
    val javaResolverCache: JavaResolverCache,
    val javaPropertyInitializerEvaluator: JavaPropertyInitializerEvaluator,
    val samConversionResolver: SamConversionResolver,
    val sourceElementFactory: JavaSourceElementFactory,
    val moduleClassResolver: ModuleClassResolver,
    val packagePartProvider: PackagePartProvider,
    val supertypeLoopChecker: SupertypeLoopChecker,
    val lookupTracker: LookupTracker,
    val module: ModuleDescriptor,
    val reflectionTypes: ReflectionTypes,
    val annotationTypeQualifierResolver: AnnotationTypeQualifierResolver,
    val signatureEnhancement: SignatureEnhancement,
    val javaClassesTracker: JavaClassesTracker,
    val settings: JavaResolverSettings
) {
    fun replace(
        javaResolverCache: JavaResolverCache = this.javaResolverCache
    ) = JavaResolverComponents(
        storageManager, finder, kotlinClassFinder, deserializedDescriptorResolver,
        signaturePropagator, errorReporter, javaResolverCache,
        javaPropertyInitializerEvaluator, samConversionResolver, sourceElementFactory,
        moduleClassResolver, packagePartProvider, supertypeLoopChecker, lookupTracker, module, reflectionTypes,
        annotationTypeQualifierResolver, signatureEnhancement, javaClassesTracker, settings
    )
}

interface JavaResolverSettings {
    val isReleaseCoroutines: Boolean

    object Default : JavaResolverSettings {
        override val isReleaseCoroutines: Boolean
            get() = false
    }

    companion object {
        fun create(isReleaseCoroutines: Boolean): JavaResolverSettings =
            object : JavaResolverSettings {
                override val isReleaseCoroutines get() = isReleaseCoroutines
            }
    }
}

typealias QualifierByApplicabilityType = EnumMap<AnnotationTypeQualifierResolver.QualifierApplicabilityType, NullabilityQualifierWithMigrationStatus?>

class JavaTypeQualifiersByElementType(val nullabilityQualifiers: QualifierByApplicabilityType) {
    operator fun get(applicabilityType: AnnotationTypeQualifierResolver.QualifierApplicabilityType?): JavaTypeQualifiers? {
        val nullabilityQualifierWithMigrationStatus = nullabilityQualifiers[applicabilityType] ?: return null

        return JavaTypeQualifiers(
            nullabilityQualifierWithMigrationStatus.qualifier, null,
            isNotNullTypeParameter = false,
            isNullabilityQualifierForWarning = nullabilityQualifierWithMigrationStatus.isForWarningOnly
        )
    }
}

class LazyJavaResolverContext internal constructor(
    val components: JavaResolverComponents,
    val typeParameterResolver: TypeParameterResolver,
    internal val delegateForDefaultTypeQualifiers: Lazy<JavaTypeQualifiersByElementType?>
) {
    constructor(
        components: JavaResolverComponents,
        typeParameterResolver: TypeParameterResolver,
        typeQualifiersComputation: () -> JavaTypeQualifiersByElementType?
    ) : this(components, typeParameterResolver, lazy(LazyThreadSafetyMode.NONE, typeQualifiersComputation))

    val defaultTypeQualifiers: JavaTypeQualifiersByElementType? by delegateForDefaultTypeQualifiers

    val typeResolver = JavaTypeResolver(this, typeParameterResolver)

    val storageManager: StorageManager
        get() = components.storageManager

    val module: ModuleDescriptor get() = components.module
}

fun LazyJavaResolverContext.child(
    typeParameterResolver: TypeParameterResolver
) = LazyJavaResolverContext(components, typeParameterResolver, delegateForDefaultTypeQualifiers)

fun LazyJavaResolverContext.computeNewDefaultTypeQualifiers(
    additionalAnnotations: Annotations
): JavaTypeQualifiersByElementType? {
    if (components.annotationTypeQualifierResolver.disabled) return defaultTypeQualifiers

    val nullabilityQualifiersWithApplicability =
        additionalAnnotations.mapNotNull(this::extractDefaultNullabilityQualifier)

    if (nullabilityQualifiersWithApplicability.isEmpty()) return defaultTypeQualifiers

    val nullabilityQualifiersByType =
        defaultTypeQualifiers?.nullabilityQualifiers?.let(::QualifierByApplicabilityType)
            ?: QualifierByApplicabilityType(AnnotationTypeQualifierResolver.QualifierApplicabilityType::class.java)

    var wasUpdate = false
    for ((nullability, applicableTo) in nullabilityQualifiersWithApplicability) {
        for (applicabilityType in applicableTo) {
            nullabilityQualifiersByType[applicabilityType] = nullability
            wasUpdate = true
        }
    }

    return if (!wasUpdate) defaultTypeQualifiers else JavaTypeQualifiersByElementType(nullabilityQualifiersByType)
}

private fun LazyJavaResolverContext.extractDefaultNullabilityQualifier(
    annotationDescriptor: AnnotationDescriptor
): NullabilityQualifierWithApplicability? {
    val typeQualifierResolver = components.annotationTypeQualifierResolver
    typeQualifierResolver.resolveQualifierBuiltInDefaultAnnotation(annotationDescriptor)?.let { return it }

    val (typeQualifier, applicability) =
        typeQualifierResolver.resolveTypeQualifierDefaultAnnotation(annotationDescriptor)
            ?: return null

    val jsr305State = typeQualifierResolver.resolveJsr305CustomState(annotationDescriptor)
        ?: typeQualifierResolver.resolveJsr305AnnotationState(typeQualifier)

    if (jsr305State.isIgnore) {
        return null
    }

    val nullabilityQualifier =
        components
            .signatureEnhancement
            .extractNullability(typeQualifier)
            ?.copy(isForWarningOnly = jsr305State.isWarning)
            ?: return null

    return NullabilityQualifierWithApplicability(nullabilityQualifier, applicability)
}

data class NullabilityQualifierWithApplicability(
    val nullabilityQualifier: NullabilityQualifierWithMigrationStatus,
    val qualifierApplicabilityTypes: Collection<AnnotationTypeQualifierResolver.QualifierApplicabilityType>
)

fun LazyJavaResolverContext.replaceComponents(
    components: JavaResolverComponents
) = LazyJavaResolverContext(components, typeParameterResolver, delegateForDefaultTypeQualifiers)

private fun LazyJavaResolverContext.child(
    containingDeclaration: DeclarationDescriptor,
    typeParameterOwner: JavaTypeParameterListOwner?,
    typeParametersIndexOffset: Int = 0,
    delegateForTypeQualifiers: Lazy<JavaTypeQualifiersByElementType?>
) = LazyJavaResolverContext(
    components,
    typeParameterOwner?.let { LazyJavaTypeParameterResolver(this, containingDeclaration, it, typeParametersIndexOffset) }
        ?: typeParameterResolver,
    delegateForTypeQualifiers
)

fun LazyJavaResolverContext.childForMethod(
    containingDeclaration: DeclarationDescriptor,
    typeParameterOwner: JavaTypeParameterListOwner,
    typeParametersIndexOffset: Int = 0
) = child(containingDeclaration, typeParameterOwner, typeParametersIndexOffset, delegateForDefaultTypeQualifiers)

fun LazyJavaResolverContext.childForClassOrPackage(
    containingDeclaration: ClassOrPackageFragmentDescriptor,
    typeParameterOwner: JavaTypeParameterListOwner? = null,
    typeParametersIndexOffset: Int = 0
) = child(
    containingDeclaration, typeParameterOwner, typeParametersIndexOffset,
    lazy(LazyThreadSafetyMode.NONE) { computeNewDefaultTypeQualifiers(containingDeclaration.annotations) }
)

fun LazyJavaResolverContext.copyWithNewDefaultTypeQualifiers(
    additionalAnnotations: Annotations
) = when {
    additionalAnnotations.isEmpty() -> this
    else -> LazyJavaResolverContext(
        components, typeParameterResolver,
        lazy(LazyThreadSafetyMode.NONE) { computeNewDefaultTypeQualifiers(additionalAnnotations) }
    )
}
