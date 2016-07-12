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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase
import org.jetbrains.kotlin.load.java.FakePureImplementationsProvider
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.child
import org.jetbrains.kotlin.load.java.lazy.replaceComponents
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.platform.createMappedTypeParametersSubstitution
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.InnerClassesScopeWrapper
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.NotFoundClasses
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

class LazyJavaClassDescriptor(
        outerContext: LazyJavaResolverContext,
        containingDeclaration: DeclarationDescriptor,
        private val jClass: JavaClass,
        private val additionalSupertypeClassDescriptor: ClassDescriptor? = null
) : ClassDescriptorBase(outerContext.storageManager, containingDeclaration, jClass.name,
                        outerContext.components.sourceElementFactory.source(jClass)), JavaClassDescriptor {

    private val c: LazyJavaResolverContext = outerContext.child(this, jClass)

    init {
        c.components.javaResolverCache.recordClass(jClass, this)
    }

    private val kind = when {
        jClass.isAnnotationType -> ClassKind.ANNOTATION_CLASS
        jClass.isInterface -> ClassKind.INTERFACE
        jClass.isEnum -> ClassKind.ENUM_CLASS
        else -> ClassKind.CLASS
    }

    private val modality = if (jClass.isAnnotationType)
                               Modality.FINAL
                           else Modality.convertFromFlags(jClass.isAbstract || jClass.isInterface, !jClass.isFinal)

    private val visibility = jClass.visibility
    private val isInner = jClass.outerClass != null && !jClass.isStatic

    override fun getKind() = kind
    override fun getModality() = modality

    // To workaround a problem with Scala compatibility (KT-9700),
    // we consider private visibility of a Java top level class as package private
    // Shortly: Scala plugin introduces special kind of "private in package" classes
    // which can be inherited from the same package.
    // Kotlin considers this "private in package" just as "private" and thinks they are invisible for inheritors,
    // so their functions are invisible fake which is not true.
    override fun getVisibility() =
            if (visibility == Visibilities.PRIVATE && jClass.outerClass == null) JavaVisibilities.PACKAGE_VISIBILITY else visibility

    override fun isInner() = isInner
    override fun isData() = false

    private val typeConstructor = c.storageManager.createLazyValue { LazyJavaClassTypeConstructor() }
    override fun getTypeConstructor(): TypeConstructor = typeConstructor()

    private val unsubstitutedMemberScope = LazyJavaClassMemberScope(c, this, jClass)
    override fun getUnsubstitutedMemberScope() = unsubstitutedMemberScope

    private val innerClassesScope = InnerClassesScopeWrapper(getUnsubstitutedMemberScope())
    override fun getUnsubstitutedInnerClassesScope(): MemberScope = innerClassesScope

    private val staticScope = LazyJavaStaticClassScope(c, jClass, this)
    override fun getStaticScope(): MemberScope = staticScope

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = null

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = null

    override fun getConstructors() = unsubstitutedMemberScope.constructors()

    override val annotations by c.storageManager.createLazyValue { c.resolveAnnotations(jClass) }

    private val functionTypeForSamInterface = c.storageManager.createNullableLazyValue {
        c.components.samConversionResolver.resolveFunctionTypeIfSamInterface(this)
    }

    private val declaredParameters = c.storageManager.createLazyValue {
        jClass.typeParameters.map {
            p ->
            c.typeParameterResolver.resolveTypeParameter(p)
                ?: throw AssertionError("Parameter $p surely belongs to class $jClass, so it must be resolved")
        }
    }

    override fun getDeclaredTypeParameters() = declaredParameters()

    override fun getFunctionTypeForSamInterface(): SimpleType? = functionTypeForSamInterface()

    override fun isCompanionObject() = false

    override fun toString() = "Lazy Java class ${this.fqNameUnsafe}"

    private inner class LazyJavaClassTypeConstructor : AbstractClassTypeConstructor(c.storageManager) {
        private val parameters = c.storageManager.createLazyValue {
            this@LazyJavaClassDescriptor.computeConstructorTypeParameters()
        }

        override fun getParameters(): List<TypeParameterDescriptor> = parameters()

        override fun computeSupertypes(): Collection<KotlinType> {
            val javaTypes = jClass.supertypes
            val result = ArrayList<KotlinType>(javaTypes.size)
            val incomplete = ArrayList<JavaType>(0)

            val purelyImplementedSupertype: KotlinType? = getPurelyImplementedSupertype()

            for (javaType in javaTypes) {
                val kotlinType = c.typeResolver.transformJavaType(javaType, TypeUsage.SUPERTYPE.toAttributes())
                if (kotlinType.constructor.declarationDescriptor is NotFoundClasses.MockClassDescriptor) {
                    incomplete.add(javaType)
                }

                if (kotlinType.constructor == purelyImplementedSupertype?.constructor) {
                    continue
                }

                if (!KotlinBuiltIns.isAnyOrNullableAny(kotlinType)) {
                    result.add(kotlinType)
                }
            }

            // Add fake supertype kotlin.collection.Collection<E> to java.util.Collection<E> class if needed
            // Only needed when calculating built-ins member scope
            result.addIfNotNull(
                    additionalSupertypeClassDescriptor?.let {
                        createMappedTypeParametersSubstitution(it, this@LazyJavaClassDescriptor)
                                .buildSubstitutor().substitute(it.defaultType, Variance.INVARIANT)
                    })

            result.addIfNotNull(purelyImplementedSupertype)

            if (incomplete.isNotEmpty()) {
                c.components.errorReporter.reportIncompleteHierarchy(declarationDescriptor, incomplete.map { javaType ->
                    (javaType as JavaClassifierType).presentableText
                })
            }

            return if (result.isNotEmpty()) result.toReadOnlyList() else listOf(c.module.builtIns.anyType)
        }

        private fun getPurelyImplementedSupertype(): KotlinType? {
            val purelyImplementedFqName = getPurelyImplementsFqNameFromAnnotation()
                                          ?: FakePureImplementationsProvider.getPurelyImplementedInterface(fqNameSafe)
                                          ?: return null

            if (purelyImplementedFqName.isRoot || !purelyImplementedFqName.toUnsafe().startsWith(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) return null

            val classDescriptor = c.module.builtIns.getBuiltInClassByFqNameNullable(purelyImplementedFqName) ?: return null

            if (classDescriptor.typeConstructor.parameters.size != getTypeConstructor().parameters.size) return null

            val parametersAsTypeProjections = getTypeConstructor().parameters.map {
                parameter -> TypeProjectionImpl(Variance.INVARIANT, parameter.defaultType)
            }

            return KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, classDescriptor, parametersAsTypeProjections)
        }

        private fun getPurelyImplementsFqNameFromAnnotation(): FqName? {
            val annotation =
                    this@LazyJavaClassDescriptor.annotations.findAnnotation(JvmAnnotationNames.PURELY_IMPLEMENTS_ANNOTATION)
                    ?: return null

            val fqNameString = (annotation.allValueArguments.values.singleOrNull() as? StringValue)?.value ?: return null
            if (!isValidJavaFqName(fqNameString)) return null

            return FqName(fqNameString)
        }

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = c.components.supertypeLoopChecker

        override val annotations: Annotations get() = Annotations.EMPTY

        override fun isFinal(): Boolean = isFinalClass

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@LazyJavaClassDescriptor

        override fun toString(): String = getName().asString()
    }

    // Only needed when calculating built-ins member scope
    internal fun copy(
            javaResolverCache: JavaResolverCache, additionalSupertypeClassDescriptor: ClassDescriptor?
    ) = LazyJavaClassDescriptor(
            c.replaceComponents(c.components.replace(javaResolverCache = javaResolverCache)),
            containingDeclaration, jClass, additionalSupertypeClassDescriptor)
}
