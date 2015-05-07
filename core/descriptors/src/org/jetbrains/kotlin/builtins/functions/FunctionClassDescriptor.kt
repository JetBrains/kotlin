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

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor.Kind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.AbstractClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinClass
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.ArrayList
import java.util.EnumSet

/**
 * A [ClassDescriptor] representing the fictitious class for a function type, such as kotlin.Function1 or kotlin.reflect.KMemberFunction0.
 *
 * Classes which are represented by this descriptor include (with supertypes):
 *
 * Function1 : Function
 * KFunction1 : Function1, KFunction
 * KMemberFunction1 : Function2, KMemberFunction
 * KExtensionFunction1 : Function2, KExtensionFunction
 * (TODO) KMemberExtensionFunction1 : Function3, KMemberExtensionFunction
 */
public class FunctionClassDescriptor(
        private val storageManager: StorageManager,
        private val containingDeclaration: PackageFragmentDescriptor,
        val functionKind: Kind,
        val arity: Int
) : AbstractClassDescriptor(storageManager, functionKind.numberedClassName(arity)) {

    public enum class Kind(val packageFqName: FqName, val classNamePrefix: String) {
        Function(BUILT_INS_PACKAGE_FQ_NAME, "Function"),
        KFunction(KOTLIN_REFLECT_FQ_NAME, "KFunction"),
        KMemberFunction(KOTLIN_REFLECT_FQ_NAME, "KMemberFunction"),
        KExtensionFunction(KOTLIN_REFLECT_FQ_NAME, "KExtensionFunction");
        // TODO: KMemberExtensionFunction

        fun numberedClassName(arity: Int) = Name.identifier("$classNamePrefix$arity")
        val hasDispatchReceiver: Boolean get() = this == KMemberFunction
        val hasExtensionReceiver: Boolean get() = this == KExtensionFunction
    }

    public object Kinds {
        val Functions = EnumSet.of(Kind.Function)
        val KFunctions = EnumSet.complementOf(Functions)

        fun byPackage(fqName: FqName) = when (fqName) {
            BUILT_INS_PACKAGE_FQ_NAME -> Functions
            KOTLIN_REFLECT_FQ_NAME -> KFunctions
            else -> error(fqName)
        }
    }

    private val staticScope = StaticScopeForKotlinClass(this)
    private val typeConstructor = FunctionTypeConstructor()
    private val memberScope = FunctionClassScope(storageManager, this)

    override fun getContainingDeclaration() = containingDeclaration

    override fun getStaticScope() = staticScope

    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getScopeForMemberLookup() = memberScope

    override fun getCompanionObjectDescriptor() = null
    override fun getConstructors() = emptyList<ConstructorDescriptor>()
    override fun getKind() = ClassKind.INTERFACE
    override fun getModality() = Modality.ABSTRACT
    override fun getUnsubstitutedPrimaryConstructor() = null
    override fun getVisibility() = Visibilities.PUBLIC
    override fun isCompanionObject() = false
    override fun isInner() = false
    override fun getAnnotations() = Annotations.EMPTY
    override fun getSource() = SourceElement.NO_SOURCE

    private inner class FunctionTypeConstructor : AbstractClassTypeConstructor() {
        private val parameters: List<TypeParameterDescriptor>

        init {
            val result = ArrayList<TypeParameterDescriptor>()

            fun typeParameter(variance: Variance, name: String) {
                result.add(TypeParameterDescriptorImpl.createWithDefaultBound(
                        this@FunctionClassDescriptor, Annotations.EMPTY, false, variance, Name.identifier(name), result.size()
                ))
            }

            if (functionKind.hasDispatchReceiver) {
                typeParameter(Variance.IN_VARIANCE, "T")
            }
            if (functionKind.hasExtensionReceiver) {
                typeParameter(Variance.IN_VARIANCE, "E")
            }

            (1..arity).map { i ->
                typeParameter(Variance.IN_VARIANCE, "P$i")
            }

            typeParameter(Variance.OUT_VARIANCE, "R")

            parameters = result.toReadOnlyList()
        }

        private val supertypes = storageManager.createLazyValue {
            val result = ArrayList<JetType>(2)

            fun add(
                    packageFragment: PackageFragmentDescriptor,
                    name: Name,
                    annotations: Annotations,
                    supertypeArguments: (superParameters: List<TypeParameterDescriptor>) -> List<TypeProjection>
            ) {
                val descriptor = packageFragment.getMemberScope().getClassifier(name) as? ClassDescriptor
                                 ?: error("Class $name not found in $packageFragment")

                val typeConstructor = descriptor.getTypeConstructor()
                val arguments = supertypeArguments(typeConstructor.getParameters())

                result.add(JetTypeImpl(annotations, typeConstructor, false, arguments, descriptor.getMemberScope(arguments)))
            }

            // Add unnumbered base class, e.g. KMemberFunction for KMemberFunction5, or Function for Function0
            add(containingDeclaration, Name.identifier(functionKind.classNamePrefix), Annotations.EMPTY) { superParameters ->
                // Substitute type parameters of the super class with our type parameters with the same names
                val parametersByName = getParameters().toMap { it.getName() }
                superParameters.map { TypeProjectionImpl(parametersByName[it.getName()]!!.getDefaultType()) }
            }

            // For K*Functions, add corresponding numbered Function class, e.g. Function2 for KMemberFunction1
            if (functionKind in Kinds.KFunctions) {
                var functionArity = arity
                if (functionKind.hasDispatchReceiver) functionArity++
                if (functionKind.hasExtensionReceiver) functionArity++

                val module = containingDeclaration.getContainingDeclaration()
                val kotlinPackageFragment = module.getPackage(BUILT_INS_PACKAGE_FQ_NAME).fragments.single()

                // If this is a KMemberFunction{n} or KExtensionFunction{n}, it extends Function{n} with the annotation kotlin.extension,
                // so that the value of this type is callable as an extension function, with the receiver before the dot
                val annotations =
                        if (functionKind.hasDispatchReceiver || functionKind.hasExtensionReceiver)
                            AnnotationsImpl(listOf(KotlinBuiltIns.getInstance().createExtensionAnnotation()))
                        else Annotations.EMPTY

                add(kotlinPackageFragment, Kind.Function.numberedClassName(functionArity), annotations) {
                    // Substitute all type parameters of the super class with all our type parameters
                    getParameters().map { TypeProjectionImpl(it.getDefaultType()) }
                }
            }

            result.toReadOnlyList()
        }

        override fun getParameters() = parameters

        override fun getSupertypes(): Collection<JetType> = supertypes()

        override fun getDeclarationDescriptor() = this@FunctionClassDescriptor
        override fun isDenotable() = true
        override fun isFinal() = false
        override fun getAnnotations() = Annotations.EMPTY

        override fun toString() = getDeclarationDescriptor().toString()
    }

    override fun toString() = getName().asString()
}
