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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassMemberScope
import org.jetbrains.kotlin.load.java.lazy.replaceComponents
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.platform.createMappedTypeParametersSubstitution
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.DelegatingType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.Serializable
import java.util.*

open class BuiltInClassesAreSerializableOnJvm(
        private val moduleDescriptor: ModuleDescriptor,
        deferredOwnerModuleDescriptor: () -> ModuleDescriptor
) : AdditionalClassPartsProvider {

    private val ownerModuleDescriptor: ModuleDescriptor by lazy(deferredOwnerModuleDescriptor)

    private val mockSerializableType = createMockJavaIoSerializableType()

    private fun createMockJavaIoSerializableType(): KotlinType {
        val mockJavaIoPackageFragment = object : PackageFragmentDescriptorImpl(moduleDescriptor, FqName("java.io")) {
            override fun getMemberScope() = MemberScope.Empty
        }

        //NOTE: can't reference anyType right away, because this is sometimes called when JvmBuiltIns are initializing
        val superTypes = listOf(object : DelegatingType() {
            override fun getDelegate(): KotlinType {
                return moduleDescriptor.builtIns.anyType
            }
        })

        val mockSerializableClass = ClassDescriptorImpl(
                mockJavaIoPackageFragment, Name.identifier("Serializable"), Modality.ABSTRACT, ClassKind.INTERFACE, superTypes, SourceElement.NO_SOURCE
        )

        mockSerializableClass.initialize(MemberScope.Empty, emptySet(), null)
        return mockSerializableClass.defaultType
    }

    override fun getSupertypes(classDescriptor: DeserializedClassDescriptor): Collection<KotlinType> {
        if (isSerializableInJava(classDescriptor.fqNameSafe)) {
            return listOf(mockSerializableType)
        }
        else return listOf()
    }

    override fun getFunctions(name: Name, classDescriptor: DeserializedClassDescriptor): Collection<SimpleFunctionDescriptor> =
            getAdditionalFunctions(classDescriptor) {
                it.getContributedFunctions(name, NoLookupLocation.FROM_BUILTINS)
            }
            .map {
                additionalMember ->
                additionalMember.newCopyBuilder().apply {
                    setOwner(classDescriptor)
                    setDispatchReceiverParameter(classDescriptor.thisAsReceiverParameter)
                    setPreserveSourceElement()
                    setSubstitution(createMappedTypeParametersSubstitution(
                            additionalMember.containingDeclaration as ClassDescriptor, classDescriptor))
                }.build()!!
            }

    override fun getFunctionsNames(classDescriptor: DeserializedClassDescriptor): Collection<Name> =
            getAdditionalFunctions(classDescriptor) {
                it.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS).filterIsInstance<SimpleFunctionDescriptor>()
            }.map(SimpleFunctionDescriptor::getName)

    private fun getAdditionalFunctions(
            classDescriptor: DeserializedClassDescriptor,
            functionsByScope: (MemberScope) -> Collection<SimpleFunctionDescriptor>
    ): Collection<SimpleFunctionDescriptor> {
        // Prevents recursive dependency: memberScope(Any) -> memberScope(Object) -> memberScope(Any)
        // No additional members should be added to Any
        if (classDescriptor.isAny) return emptyList()

        val fqName = classDescriptor.fqNameUnsafe.check { it.isSafe }?.toSafe() ?: return emptyList()

        val j2kClassMap = JavaToKotlinClassMap.INSTANCE
        val javaAnalogueFqName = j2kClassMap.mapKotlinToJava(fqName.toUnsafe())?.asSingleFqName() ?: return emptyList()

        if (javaAnalogueFqName in IGNORE_BY_DEFAULT_CLASS_FQ_NAMES) return emptyList()

        val javaAnalogueDescriptor =
                ownerModuleDescriptor.resolveClassByFqName(javaAnalogueFqName, NoLookupLocation.FROM_BUILTINS) as? LazyJavaClassDescriptor
                ?: return emptyList()

        val platformClassDescriptors = j2kClassMap.mapPlatformClass(javaAnalogueDescriptor.fqNameSafe, DefaultBuiltIns.Instance)
        val kotlinMutableClassIfContainer = platformClassDescriptors.lastOrNull() ?: return emptyList()
        val platformVersions = SmartSet.create(platformClassDescriptors.map { it.fqNameSafe })

        val isMutable = j2kClassMap.isMutable(classDescriptor)

        val fakeJavaClassDescriptor =
                javaAnalogueDescriptor.copy(
                        javaResolverCache = JavaResolverCache.EMPTY,
                        additionalSupertypeClassDescriptor = kotlinMutableClassIfContainer)

        val scope = fakeJavaClassDescriptor.unsubstitutedMemberScope

        return functionsByScope(scope)
                .filter { analogueMember ->
                    if (analogueMember.kind != CallableMemberDescriptor.Kind.DECLARATION) return@filter false
                    if (!analogueMember.visibility.isPublicAPI) return@filter false

                    val methodFqName = analogueMember.fqNameSafe

                    if (methodFqName in BLACK_LIST_METHODS_FQ_NAMES) return@filter false
                    if ((methodFqName in MUTABLE_METHODS_FQ_NAMES) xor isMutable) return@filter false

                    analogueMember.overriddenDescriptors.none {
                        it.containingDeclaration.fqNameSafe in platformVersions
                    }
                }
    }

    companion object {
        fun isSerializableInJava(classFqName: FqName): Boolean {
            val fqNameUnsafe = classFqName.toUnsafe()
            if (fqNameUnsafe == KotlinBuiltIns.FQ_NAMES.array || KotlinBuiltIns.isPrimitiveArray(fqNameUnsafe)) {
                return true
            }
            val javaClassId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(fqNameUnsafe) ?: return false
            val classViaReflection = try {
                Class.forName(javaClassId.asSingleFqName().asString())
            }
            catch (e: ClassNotFoundException) {
                return false
            }
            return Serializable::class.java.isAssignableFrom(classViaReflection)
        }

        private val IGNORE_BY_DEFAULT_CLASS_FQ_NAMES =
                setOf(FqName("java.lang.String")) +
                JvmPrimitiveType.values().map { it.wrapperFqName }

        private val BLACK_LIST_METHODS_FQ_NAMES =
                buildPrimitiveValueMethodsSet() +
                FqName("java.util.Collection.toArray") +
                FqName("java.util.List.toArray") +
                FqName("java.util.Set.toArray") +
                FqName("java.lang.annotation.Annotation.annotationType")

        private val MUTABLE_METHODS_FQ_NAMES =
                inClass("java.util.Collection",
                        "removeIf") +

                inClass("java.util.List",
                        "sort", "replaceAll") +

                inClass("java.util.Map",
                        "compute", "computeIfAbsent", "computeIfPresent", "remove", "merge", "putIfAbsent", "replace", "replaceAll")

        private fun buildPrimitiveValueMethodsSet() =
                JvmPrimitiveType.values().mapTo(LinkedHashSet()) {
                    it.wrapperFqName.child(Name.identifier(it.javaKeywordName + "Value"))
                }

        private fun inClass(classFqName: String, vararg names: String) =
                names.mapTo(LinkedHashSet()) { FqName(classFqName).child(Name.identifier(it)) }
    }
}

private val ClassDescriptor.isAny: Boolean get() = fqNameUnsafe == KotlinBuiltIns.FQ_NAMES.any