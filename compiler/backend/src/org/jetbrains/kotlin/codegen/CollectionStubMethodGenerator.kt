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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.getSpecialSignatureInfo
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.isBuiltinWithSpecialDescriptorInJvm
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.NonReportingOverrideStrategy
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.resolve.OverridingStrategy
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

/**
 * Generates exception-throwing stubs for methods from mutable collection classes not implemented in Kotlin classes which inherit only from
 * Kotlin's read-only collections. This is required on JVM because Kotlin's read-only collections are mapped to mutable JDK collections
 */
class CollectionStubMethodGenerator(
        state: GenerationState,
        private val descriptor: ClassDescriptor,
        private val functionCodegen: FunctionCodegen,
        private val v: ClassBuilder
) {
    private val typeMapper = state.typeMapper

    fun generate() {
        val superCollectionClasses = findRelevantSuperCollectionClasses()
        if (superCollectionClasses.isEmpty()) return

        val methodStubsToGenerate = LinkedHashSet<JvmMethodSignature>()
        val syntheticStubsToGenerate = LinkedHashSet<JvmMethodSignature>()

        for ((readOnlyClass, mutableClass) in superCollectionClasses) {
            // To determine which method stubs we need to generate, we create a synthetic class (named 'child' here) which inherits from
            // our class ('descriptor') and the corresponding MutableCollection class (for example; the process is the same for every
            // built-in read-only/mutable class pair). We then construct and bind fake overrides in this synthetic class with the usual
            // override resolution process. Resulting fake overrides with originals in MutableCollection are considered as candidates for
            // method stubs or bridges to the actual implementation that happened to be present in the class
            val (child, typeParameters) = createSyntheticSubclass()
            // If the original class has any type parameters, we copied them and now we need to substitute types of the newly created type
            // parameters as arguments for the type parameters of the original class
            val parentType = newType(descriptor, typeParameters.map { TypeProjectionImpl(it.defaultType) })

            // Now we need to determine the arguments which should be substituted for the MutableCollection super class. To do that,
            // we look for type arguments which were substituted in the inheritance of the original class from Collection and use them
            // to construct the needed MutableCollection type. Since getAllSupertypes() may return several types which correspond to the
            // Collection class descriptor, we find the most specific one (which is guaranteed to exist by front-end)
            val readOnlyCollectionType = TypeUtils.getAllSupertypes(parentType).findMostSpecificTypeForClass(readOnlyClass)
            val mutableCollectionType = newType(mutableClass, readOnlyCollectionType.arguments)

            child.addSupertype(parentType)
            child.addSupertype(mutableCollectionType)
            child.createTypeConstructor()

            // Bind fake overrides and for each fake override originated from the MutableCollection, save its signature to generate a stub
            // or save its descriptor to generate all the needed bridges
            for (method in findFakeOverridesForMethodsFromMutableCollection(child, mutableClass)) {
                if (method.modality == Modality.ABSTRACT) {
                    // If the fake override is abstract and it's _declared_ as abstract in the class, skip it because the method is already
                    // present in the bytecode (abstract) and we don't want a duplicate signature error
                    if (method.findOverriddenFromDirectSuperClass(descriptor)?.kind == DECLARATION) continue

                    // Otherwise we can safely generate the stub with the substituted signature
                    val signature = method.signature()
                    methodStubsToGenerate.add(signature)

                    // If the substituted signature differs from the original one in MutableCollection, we should also generate a stub with
                    // the original (erased) signature. It doesn't really matter if this is a bridge method delegating to the first stub or
                    // a method with its own exception-throwing code, for simplicity we do the latter here.
                    // What _does_ matter though, is that these two methods can't be both non-synthetic at once: javac issues compilation
                    // errors when compiling Java against such classes because one of them doesn't seem to override the generic method
                    // declared in the Java Collection interface (can't override generic with erased). So we maintain an additional set of
                    // methods which need to be generated with the ACC_SYNTHETIC flag
                    val overriddenMethod = method.findOverriddenFromDirectSuperClass(mutableClass)!!
                    val originalSignature = overriddenMethod.original.signature()
                    var specialSignature: JvmMethodSignature? = null

                    if (overriddenMethod.isBuiltinWithSpecialDescriptorInJvm()) {
                        // Stubs for remove(Ljava/lang/Object;)Z and remove(I) should not be synthetic
                        // Otherwise Javac will not see it
                        val overriddenMethodSignature = overriddenMethod.signature()
                        val genericSignatureInfo = overriddenMethod.getSpecialSignatureInfo()

                        val specialGenericSignature =
                                genericSignatureInfo?.replaceValueParametersIn(overriddenMethodSignature.genericsSignature)
                                ?: overriddenMethodSignature.genericsSignature

                        val (asmMethod, valueParameters) =
                                // if current method has special generic signature,
                                // like `Collection.remove(E): Boolean` in Kotlin, use original signature to obtain `remove(Object)`
                                if (genericSignatureInfo?.isObjectReplacedWithTypeParameter ?: false)
                                    Pair(originalSignature.asmMethod, originalSignature.valueParameters)
                                else
                                    Pair(overriddenMethodSignature.asmMethod, overriddenMethodSignature.valueParameters)

                        specialSignature = JvmMethodSignature(
                                asmMethod,
                                specialGenericSignature,
                                valueParameters
                        )

                        methodStubsToGenerate.add(specialSignature)
                    }

                    if (originalSignature.asmMethod != signature.asmMethod && originalSignature.asmMethod != specialSignature?.asmMethod) {
                        syntheticStubsToGenerate.add(originalSignature)
                    }
                }
                else {
                    // If the fake override is non-abstract, its implementation is already present in the class or inherited from one of its
                    // super classes, but is not related to the MutableCollection hierarchy. So maybe it uses more specific return types
                    // and we may need to generate some bridges
                    functionCodegen.generateBridges(method)
                }
            }
        }

        for (signature in methodStubsToGenerate) {
            generateMethodStub(signature, synthetic = false)
        }

        for (signature in syntheticStubsToGenerate) {
            generateMethodStub(signature, synthetic = true)
        }
    }

    private data class CollectionClassPair(
            val readOnlyClass: ClassDescriptor,
            val mutableClass: ClassDescriptor
    )

    private fun findRelevantSuperCollectionClasses(): Collection<CollectionClassPair> {
        fun pair(readOnlyClass: ClassDescriptor, mutableClass: ClassDescriptor) = CollectionClassPair(readOnlyClass, mutableClass)

        val collectionClasses = with(descriptor.builtIns) {
            listOf(
                    pair(collection, mutableCollection),
                    pair(set, mutableSet),
                    pair(list, mutableList),
                    pair(map, mutableMap),
                    pair(mapEntry, mutableMapEntry),
                    pair(iterable, mutableIterable),
                    pair(iterator, mutableIterator),
                    pair(listIterator, mutableListIterator)
            )
        }

        val allSuperClasses = TypeUtils.getAllSupertypes(descriptor.defaultType).classes().toHashSet()

        val ourSuperCollectionClasses = collectionClasses.filter { pair ->
            pair.readOnlyClass in allSuperClasses && pair.mutableClass !in allSuperClasses
        }
        if (ourSuperCollectionClasses.isEmpty()) return listOf()

        // Filter out built-in classes which are overridden by other built-in classes in the list, to avoid duplicating methods.
        val redundantClasses = ourSuperCollectionClasses.flatMapTo(HashSet<ClassDescriptor>()) { pair ->
            pair.readOnlyClass.typeConstructor.supertypes.classes()
        }
        return ourSuperCollectionClasses.filter { klass -> klass.readOnlyClass !in redundantClasses }
    }

    private fun Collection<KotlinType>.classes(): Collection<ClassDescriptor> =
            this.map { it.constructor.declarationDescriptor as ClassDescriptor }

    private fun findFakeOverridesForMethodsFromMutableCollection(
            klass: ClassDescriptor,
            mutableCollectionClass: ClassDescriptor
    ): List<FunctionDescriptor> {
        val result = ArrayList<FunctionDescriptor>()

        OverrideResolver.generateOverridesInAClass(klass, listOf(), object : NonReportingOverrideStrategy() {
            override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                if (fakeOverride !is FunctionDescriptor) return
                if (fakeOverride.findOverriddenFromDirectSuperClass(mutableCollectionClass)?.kind == DECLARATION) {
                    result.add(fakeOverride)
                }
            }

            override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                // Ignore conflicts here
                // TODO: report a warning that javac will prohibit use/inheritance from such class
            }
        })

        return result
    }

    private fun Collection<KotlinType>.findMostSpecificTypeForClass(klass: ClassDescriptor): KotlinType {
        val types = this.filter { it.constructor.declarationDescriptor == klass }
        if (types.isEmpty()) error("No supertype of $klass in $this")
        if (types.size == 1) return types.first()
        // Find the first type in the list such that it's a subtype of every other type in that list
        return types.first { type ->
            types.all { other -> KotlinTypeChecker.DEFAULT.isSubtypeOf(type, other) }
        }
    }

    private fun createSyntheticSubclass(): Pair<MutableClassDescriptor, List<TypeParameterDescriptor>> {
        val child = MutableClassDescriptor(descriptor.containingDeclaration, ClassKind.CLASS, false,
                                           Name.special("<synthetic inheritor of ${descriptor.name}>"), descriptor.source)
        child.modality = Modality.FINAL
        child.visibility = Visibilities.PUBLIC
        val typeParameters = descriptor.typeConstructor.parameters
        val newTypeParameters = ArrayList<TypeParameterDescriptor>(typeParameters.size)
        DescriptorSubstitutor.substituteTypeParameters(typeParameters, TypeSubstitution.EMPTY, child, newTypeParameters)
        child.setTypeParameterDescriptors(typeParameters)
        return Pair(child, newTypeParameters)
    }

    private fun FunctionDescriptor.findOverriddenFromDirectSuperClass(classDescriptor: ClassDescriptor): FunctionDescriptor? {
        return this.overriddenDescriptors.firstOrNull { it.containingDeclaration == classDescriptor }
    }

    private fun newType(classDescriptor: ClassDescriptor, typeArguments: List<TypeProjection>): KotlinType {
        return KotlinTypeImpl.create(Annotations.EMPTY, classDescriptor, false, typeArguments)
    }

    private fun FunctionDescriptor.signature(): JvmMethodSignature = typeMapper.mapSignature(this)

    private fun generateMethodStub(signature: JvmMethodSignature, synthetic: Boolean) {
        // TODO: investigate if it makes sense to generate abstract stubs in traits
        var access = ACC_PUBLIC
        if (descriptor.kind == ClassKind.INTERFACE) access = access or ACC_ABSTRACT
        if (synthetic) access = access or ACC_SYNTHETIC

        val asmMethod = signature.asmMethod
        val genericSignature = if (synthetic) null else signature.genericsSignature
        val mv = v.newMethod(JvmDeclarationOrigin.NO_ORIGIN, access, asmMethod.name, asmMethod.descriptor, genericSignature, null)
        if (descriptor.kind != ClassKind.INTERFACE) {
            mv.visitCode()
            AsmUtil.genThrow(InstructionAdapter(mv), "java/lang/UnsupportedOperationException", "Mutating immutable collection")
            FunctionCodegen.endVisit(mv, "built-in stub for $signature", null)
        }
    }
}
