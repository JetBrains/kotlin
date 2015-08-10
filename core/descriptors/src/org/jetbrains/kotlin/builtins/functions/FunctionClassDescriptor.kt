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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor.Kind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinClass
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.ArrayList

/**
 * A [ClassDescriptor] representing the fictitious class for a function type, such as kotlin.Function1 or kotlin.reflect.KFunction2.
 *
 * If the class represents kotlin.Function1, its only supertype is kotlin.Function.
 *
 * If the class represents kotlin.reflect.KFunction1, it has two supertypes: kotlin.Function1 and kotlin.reflect.KFunction.
 * This allows to use both 'invoke' and reflection API on function references obtained by '::'.
 */
public class FunctionClassDescriptor(
        private val storageManager: StorageManager,
        private val containingDeclaration: PackageFragmentDescriptor,
        val functionKind: Kind,
        val arity: Int
) : AbstractClassDescriptor(storageManager, functionKind.numberedClassName(arity)) {

    public enum class Kind(val packageFqName: FqName, val classNamePrefix: String) {
        Function(BUILT_INS_PACKAGE_FQ_NAME, "Function"),
        KFunction(KOTLIN_REFLECT_FQ_NAME, "KFunction");

        fun numberedClassName(arity: Int) = Name.identifier("$classNamePrefix$arity")

        companion object {
            fun byPackage(fqName: FqName) = when (fqName) {
                BUILT_INS_PACKAGE_FQ_NAME -> Function
                KOTLIN_REFLECT_FQ_NAME -> KFunction
                else -> null
            }
        }
    }

    private val staticScope = StaticScopeForKotlinClass(this)
    private val typeConstructor = FunctionTypeConstructor()
    private val memberScope = FunctionClassScope(storageManager, this)

    override fun getContainingDeclaration() = containingDeclaration

    override fun getStaticScope() = staticScope

    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getUnsubstitutedMemberScope() = memberScope

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

            (1..arity).map { i ->
                typeParameter(Variance.IN_VARIANCE, "P$i")
            }

            typeParameter(Variance.OUT_VARIANCE, "R")

            parameters = result.toReadOnlyList()
        }

        private val supertypes = storageManager.createLazyValue {
            val result = ArrayList<JetType>(2)

            fun add(packageFragment: PackageFragmentDescriptor, name: Name) {
                val descriptor = packageFragment.getMemberScope().getClassifier(name) as? ClassDescriptor
                                 ?: error("Class $name not found in $packageFragment")

                val typeConstructor = descriptor.getTypeConstructor()

                // Substitute all type parameters of the super class with our last type parameters
                val arguments = getParameters().takeLast(typeConstructor.getParameters().size()).map {
                    TypeProjectionImpl(it.getDefaultType())
                }

                result.add(JetTypeImpl.create(Annotations.EMPTY, descriptor, false, arguments))
            }

            // Add unnumbered base class, e.g. Function for Function{n}, KFunction for KFunction{n}
            add(containingDeclaration, Name.identifier(functionKind.classNamePrefix))

            // For KFunction{n}, add corresponding numbered Function{n} class, e.g. Function2 for KFunction2
            if (functionKind == Kind.KFunction) {
                val module = containingDeclaration.getContainingDeclaration()
                val kotlinPackageFragment = module.getPackage(BUILT_INS_PACKAGE_FQ_NAME).fragments.single()

                add(kotlinPackageFragment, Kind.Function.numberedClassName(arity))
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
