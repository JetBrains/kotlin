/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

/**
 * A [ClassDescriptor] representing the fictitious class for a function type, such as kotlin.Function1 or kotlin.reflect.KFunction2.
 *
 * If the class represents kotlin.Function1, its only supertype is kotlin.Function.
 *
 * If the class represents kotlin.reflect.KFunction1, it has two supertypes: kotlin.Function1 and kotlin.reflect.KFunction.
 * This allows to use both 'invoke' and reflection API on function references obtained by '::'.
 */
class FunctionClassDescriptor(
        private val storageManager: StorageManager,
        private val containingDeclaration: PackageFragmentDescriptor,
        val functionKind: FunctionClassKind,
        val arity: Int
) : AbstractClassDescriptor(storageManager, functionKind.numberedClassName(arity)) {

    private val typeConstructor = FunctionTypeConstructor()
    private val memberScope = FunctionClassScope(storageManager, this)

    private val parameters: List<TypeParameterDescriptor>

    init {
        val result = ArrayList<TypeParameterDescriptor>()

        fun typeParameter(variance: Variance, name: String) {
            result.add(TypeParameterDescriptorImpl.createWithDefaultBound(
                    this@FunctionClassDescriptor, Annotations.EMPTY, false, variance, Name.identifier(name), result.size, storageManager
            ))
        }

        (1..arity).map { i ->
            typeParameter(Variance.IN_VARIANCE, "P$i")
        }

        typeParameter(Variance.OUT_VARIANCE, "R")

        parameters = result.toList()
    }

    @get:JvmName("hasBigArity")
    val hasBigArity: Boolean
        get() = arity >= BuiltInFunctionArity.BIG_ARITY

    override fun getContainingDeclaration() = containingDeclaration

    override fun getStaticScope() = MemberScope.Empty

    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getUnsubstitutedMemberScope(kotlinTypeRefiner: KotlinTypeRefiner) = memberScope

    override fun getCompanionObjectDescriptor() = null
    override fun getConstructors() = emptyList<ClassConstructorDescriptor>()
    override fun getKind() = ClassKind.INTERFACE
    override fun getModality() = Modality.ABSTRACT
    override fun getUnsubstitutedPrimaryConstructor() = null
    override fun getVisibility() = DescriptorVisibilities.PUBLIC
    override fun isCompanionObject() = false
    override fun isInner() = false
    override fun isData() = false
    override fun isInline() = false
    override fun isFun() = false
    override fun isValue() = false
    override fun isExpect() = false
    override fun isActual() = false
    override fun isExternal() = false
    override val annotations: Annotations get() = Annotations.EMPTY
    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getSealedSubclasses() = emptyList<ClassDescriptor>()
    override fun getInlineClassRepresentation(): InlineClassRepresentation<SimpleType>? = null
    override fun getMultiFieldValueClassRepresentation(): MultiFieldValueClassRepresentation<SimpleType>? = null

    override fun getDeclaredTypeParameters() = parameters

    private inner class FunctionTypeConstructor : AbstractClassTypeConstructor(storageManager) {
        override fun computeSupertypes(): Collection<KotlinType> {
            // For K{Suspend}Function{n}, add corresponding numbered {Suspend}Function{n} class, e.g. {Suspend}Function2 for K{Suspend}Function2
            val supertypes = when (functionKind) {
                FunctionClassKind.Function -> // Function$N <: Function
                    listOf(functionClassId)
                FunctionClassKind.KFunction -> // KFunction$N <: KFunction
                    listOf(kFunctionClassId, ClassId(BUILT_INS_PACKAGE_FQ_NAME, FunctionClassKind.Function.numberedClassName(arity)))
                FunctionClassKind.SuspendFunction -> // SuspendFunction$N<...> <: Function
                    listOf(functionClassId)
                FunctionClassKind.KSuspendFunction -> // KSuspendFunction$N<...> <: KFunction
                    listOf(kFunctionClassId, ClassId(COROUTINES_PACKAGE_FQ_NAME, FunctionClassKind.SuspendFunction.numberedClassName(arity)))
            }

            val moduleDescriptor = containingDeclaration.containingDeclaration
            return supertypes.map { id ->
                val descriptor = moduleDescriptor.findClassAcrossModuleDependencies(id) ?: error("Built-in class $id not found")

                // Substitute all type parameters of the super class with our last type parameters
                val arguments = parameters.takeLast(descriptor.typeConstructor.parameters.size).map {
                    TypeProjectionImpl(it.defaultType)
                }

                KotlinTypeFactory.simpleNotNullType(TypeAttributes.Empty, descriptor, arguments)
            }.toList()
        }

        override fun getParameters() = this@FunctionClassDescriptor.parameters

        override fun getDeclarationDescriptor() = this@FunctionClassDescriptor
        override fun isDenotable() = true

        override fun toString() = declarationDescriptor.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = SupertypeLoopChecker.EMPTY
    }

    override fun toString() = name.asString()

    companion object {
        private val functionClassId = ClassId(BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("Function"))
        private val kFunctionClassId = ClassId(KOTLIN_REFLECT_FQ_NAME, Name.identifier("KFunction"))
    }
}
