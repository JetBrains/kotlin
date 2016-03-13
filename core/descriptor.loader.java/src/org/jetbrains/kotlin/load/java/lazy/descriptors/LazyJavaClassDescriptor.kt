/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.child
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.scopes.InnerClassesScopeWrapper
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

class LazyJavaClassDescriptor(
        private val outerC: LazyJavaResolverContext,
        containingDeclaration: DeclarationDescriptor,
        internal val fqName: FqName,
        private val jClass: JavaClass
) : ClassDescriptorBase(outerC.storageManager, containingDeclaration, fqName.shortName(),
                        outerC.components.sourceElementFactory.source(jClass)), JavaClassDescriptor {

    private val c: LazyJavaResolverContext = outerC.child(this, jClass)

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

    private val annotations = c.storageManager.createLazyValue { c.resolveAnnotations(jClass) }
    override fun getAnnotations() = annotations()

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

    override fun getFunctionTypeForSamInterface(): KotlinType? = functionTypeForSamInterface()

    override fun isCompanionObject() = false

    override fun toString() = "lazy java class $fqName"

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
                val jetType = c.typeResolver.transformJavaType(javaType, TypeUsage.SUPERTYPE.toAttributes())
                if (jetType.isError) {
                    incomplete.add(javaType)
                    continue
                }

                if (jetType.constructor == purelyImplementedSupertype?.constructor) {
                    continue
                }

                if (!KotlinBuiltIns.isAnyOrNullableAny(jetType)) {
                    result.add(jetType)
                }
            }

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
                                          ?: FakePureImplementationsProvider.getPurelyImplementedInterface(fqName)
                                          ?: return null

            if (purelyImplementedFqName.isRoot || !purelyImplementedFqName.toUnsafe().startsWith(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) return null

            val classDescriptor = c.module.builtIns.getBuiltInClassByFqNameNullable(purelyImplementedFqName) ?: return null

            if (classDescriptor.typeConstructor.parameters.size != getTypeConstructor().parameters.size) return null

            val parametersAsTypeProjections = getTypeConstructor().parameters.map {
                parameter -> TypeProjectionImpl(Variance.INVARIANT, parameter.defaultType)
            }

            return KotlinTypeImpl.create(
                    Annotations.EMPTY, classDescriptor,
                    /* nullable =*/ false, parametersAsTypeProjections
            )
        }

        private fun getPurelyImplementsFqNameFromAnnotation(): FqName? {
            val annotation =
                    this@LazyJavaClassDescriptor.getAnnotations().findAnnotation(JvmAnnotationNames.PURELY_IMPLEMENTS_ANNOTATION)
                    ?: return null

            val fqNameString = (annotation.allValueArguments.values.singleOrNull() as? StringValue)?.value ?: return null
            if (!isValidJavaFqName(fqNameString)) return null

            return FqName(fqNameString)
        }

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = c.components.supertypeLoopChecker

        override fun getAnnotations() = Annotations.EMPTY

        override fun isFinal(): Boolean = isFinalClass

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@LazyJavaClassDescriptor

        override fun toString(): String = getName().asString()
    }
}
