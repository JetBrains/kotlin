/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.TestIrBuiltins
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.util.IrErrorModuleFragment
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * [AbstractIrTypeSubstitutor.forType] matches the type parameters found by [extractTypeParameters] with the arguments of the type
 * by position, and the type does not always have exactly as many arguments.
 *
 * In particular, it has to accept the type that [starProjectedType] builds for a class:
 * `UpgradeCallableReferences.wrapFunction` passes exactly such a type to [AbstractIrTypeSubstitutor.forSuperClass].
 * [starProjectedType] takes the number of arguments from [typeConstructorParameters], which stops at an anonymous object
 * and at a property accessor, where [extractTypeParameters] goes on.
 */
class IrTypeSubstitutorTest {
    private val pkg = IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(), FqName("test"), IrErrorModuleFragment)

    /** `fun <T> foo()` */
    private fun buildGenericFunction(): IrSimpleFunction =
        IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }.apply {
            parent = pkg
            addTypeParameter("T", TestIrBuiltins.anyNType)
        }

    /** `class Outer<T>` */
    private fun buildGenericClass(): IrClass =
        IrFactoryImpl.buildClass {
            name = Name.identifier("Outer")
        }.apply {
            parent = pkg
            addTypeParameter("T", TestIrBuiltins.anyNType)
            createThisReceiverParameter()
        }

    /** `fun foo()` declared in [this] class. */
    private fun IrClass.addMemberFunction(): IrSimpleFunction =
        IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }.also { function ->
            function.parent = this
            function.parameters += function.createDispatchReceiverParameterWithClassParent()
            declarations += function
        }

    /** The getter of `val p: Unit` declared in [this] class. */
    private fun IrClass.addPropertyGetter(): IrSimpleFunction =
        addProperty {
            name = Name.identifier("p")
        }.addGetter {
            returnType = TestIrBuiltins.unitType
        }.also { getter ->
            getter.parameters += getter.createDispatchReceiverParameterWithClassParent()
        }

    private fun buildLocalClass(name: Name, parent: IrDeclarationParent): IrClass =
        IrFactoryImpl.buildClass {
            this.name = name
            visibility = DescriptorVisibilities.LOCAL
        }.also {
            it.parent = parent
        }

    private fun assertStarProjectedTypeIsAccepted(irClass: IrClass) {
        val starProjectedType = irClass.symbol.starProjectedType
        assertDoesNotThrow { AbstractIrTypeSubstitutor.forType(starProjectedType) }
        // The shape of the call in `UpgradeCallableReferences.wrapFunction`.
        assertDoesNotThrow { AbstractIrTypeSubstitutor.forSuperClass(irClass.symbol, starProjectedType) }
    }

    /**
     * ```
     * fun <T> foo() {
     *     object {}
     * }
     * ```
     */
    @Test
    fun `star-projected type of an anonymous object in a generic function`() {
        assertStarProjectedTypeIsAccepted(buildLocalClass(SpecialNames.NO_NAME_PROVIDED, parent = buildGenericFunction()))
    }

    /**
     * ```
     * class Outer<T> {
     *     fun foo() {
     *         object {}
     *     }
     * }
     * ```
     */
    @Test
    fun `star-projected type of an anonymous object in a member function of a generic class`() {
        assertStarProjectedTypeIsAccepted(buildLocalClass(SpecialNames.NO_NAME_PROVIDED, parent = buildGenericClass().addMemberFunction()))
    }

    /**
     * ```
     * class Outer<T> {
     *     val p: Unit
     *         get() {
     *             class Local
     *         }
     * }
     * ```
     */
    @Test
    fun `star-projected type of a local class in a property getter of a generic class`() {
        assertStarProjectedTypeIsAccepted(buildLocalClass(Name.identifier("Local"), parent = buildGenericClass().addPropertyGetter()))
    }

    /**
     * Both ways of collecting type parameters agree on this class, so it works regardless of which one is used.
     *
     * ```
     * fun <T> foo() {
     *     class Local
     * }
     * ```
     */
    @Test
    fun `star-projected type of a local class in a generic function`() {
        assertStarProjectedTypeIsAccepted(buildLocalClass(Name.identifier("Local"), parent = buildGenericFunction()))
    }

    /**
     * The class and the type parameters for the tests below: `L` is the own type parameter of `Local`, `T` is the captured one.
     *
     * ```
     * fun <T> foo() {
     *     class Local<L>
     * }
     * ```
     */
    private class GenericLocalClass(val irClass: IrClass, val ownTypeParameter: IrTypeParameter, val capturedTypeParameter: IrTypeParameter)

    private fun buildGenericLocalClassInGenericFunction(): GenericLocalClass {
        val function = buildGenericFunction()
        val localClass = buildLocalClass(Name.identifier("Local"), parent = function)
        val ownTypeParameter = localClass.addTypeParameter("L", TestIrBuiltins.anyNType)
        return GenericLocalClass(localClass, ownTypeParameter, capturedTypeParameter = function.typeParameters.single())
    }

    /** `Local<Int, String>`, the way the frontend builds the type. */
    @Test
    fun `own and captured type parameters are substituted if the type has arguments for both`() {
        val local = buildGenericLocalClassInGenericFunction()
        val type = local.irClass.symbol.typeWith(TestIrBuiltins.intType, TestIrBuiltins.stringType)

        val substitutor = AbstractIrTypeSubstitutor.forType(type)

        assertEquals(TestIrBuiltins.intType, substitutor.substitute(local.ownTypeParameter.defaultType))
        assertEquals(TestIrBuiltins.stringType, substitutor.substitute(local.capturedTypeParameter.defaultType))
    }

    /** `Local<Int>` */
    @Test
    fun `captured type parameter is not substituted if the type has no argument for it`() {
        val local = buildGenericLocalClassInGenericFunction()
        val type = local.irClass.symbol.typeWith(TestIrBuiltins.intType)

        val substitutor = AbstractIrTypeSubstitutor.forType(type)

        assertEquals(TestIrBuiltins.intType, substitutor.substitute(local.ownTypeParameter.defaultType))
        assertEquals(local.capturedTypeParameter.defaultType, substitutor.substitute(local.capturedTypeParameter.defaultType))
    }

    /**
     * `Local<Int, String, Boolean>`: the type as it stays after a lowering has moved the class to a declaration
     * with fewer type parameters to capture.
     */
    @Test
    fun `extra arguments are ignored if the type has more arguments than there are type parameters`() {
        val local = buildGenericLocalClassInGenericFunction()
        val type = local.irClass.symbol.typeWith(TestIrBuiltins.intType, TestIrBuiltins.stringType, TestIrBuiltins.booleanType)

        val substitutor = assertDoesNotThrow { AbstractIrTypeSubstitutor.forType(type) }

        assertEquals(TestIrBuiltins.intType, substitutor.substitute(local.ownTypeParameter.defaultType))
    }
}
