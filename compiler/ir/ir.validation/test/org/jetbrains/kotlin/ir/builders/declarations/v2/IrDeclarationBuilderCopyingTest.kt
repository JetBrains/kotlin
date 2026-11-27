/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations.v2

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.TestIrBuiltins
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.expressions.v2.buildAnnotation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IrDeclarationBuilderCopyingTest {
    private val factory = IrFactoryImpl
    private val builtIns = TestIrBuiltins

    private val comparable: IrClass = factory.irClass {
        name = Name.identifier("Comparable")
        kind = ClassKind.INTERFACE
        typeParameter("T", builtIns.anyNType)
        thisReceiver()
    }

    private val annotationClass: IrClass = factory.irClass {
        name = Name.identifier("Anno")
        kind = ClassKind.ANNOTATION_CLASS
        thisReceiver()
    }.apply { irConstructor() }

    /** `fun <T : Comparable<T>, R : T> f(context(c: Int), @Anno vararg x: T = 0): R` */
    private fun sourceFunction(): IrSimpleFunction =
        factory.irFunction {
            name = Name.identifier("f")
            isInline = true
            val t = typeParameter { name = Name.identifier("T") }
            val r = typeParameter("R", t.defaultType)
            returnType = r.defaultType
            contextParameter {
                name = Name.identifier("c")
                type = builtIns.intType
            }
            valueParameter {
                name = Name.identifier("x")
                type = builtIns.arrayClass.typeWith(t.defaultType)
                varargElementType = t.defaultType
                annotations = listOf(
                    buildAnnotation(annotationClass.constructors.single().symbol) { type = annotationClass.defaultType }
                )
            }
        }.apply {
            // A self-reference can't be expressed in the builder of the type parameter.
            val t = typeParameters[0]
            t.superTypes = listOf(comparable.symbol.typeWith(t.defaultType))
            parameters[1].defaultValue = factory.createExpressionBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.intType, 0)
            )
        }

    @Test
    fun copySignature() {
        val source = sourceFunction()
        val mapping: IrSignatureMapping
        val copy = factory.irFunction {
            copyFrom(source)
            name = Name.identifier("g")
            mapping = copySignatureFrom(source)
        }

        assertEquals("g", copy.name.asString())
        assertEquals(true, copy.isInline)

        val [t, r] = copy.typeParameters
        assertEquals(listOf("T", "R"), copy.typeParameters.map { it.name.asString() })
        assertEquals(listOf(0, 1), copy.typeParameters.map { it.index })
        assertEquals(listOf(comparable.symbol.typeWith(t.defaultType)), t.superTypes)
        assertEquals(listOf<IrType>(t.defaultType), r.superTypes)
        assertEquals(r.defaultType, copy.returnType)
        assertEquals(
            source.typeParameters.map { it.symbol }.zip(copy.typeParameters.map { it.defaultType }).toMap(),
            mapping.typeSubstitution,
        )

        assertEquals(listOf(IrParameterKind.Context, IrParameterKind.Regular), copy.parameters.map { it.kind })
        val x = copy.parameters[1]
        assertEquals(builtIns.arrayClass.typeWith(t.defaultType), x.type)
        assertEquals(t.defaultType, x.varargElementType)
        assertNull(x.defaultValue)
        assertEquals(annotationClass.symbol, x.annotations.single().type.classOrNull)
        assertNotSame(source.parameters[1].annotations.single(), x.annotations.single())
        assertEquals(source.parameters.map { it.symbol }.zip(copy.parameters.map { it.symbol }).toMap(), mapping.parameters)
    }

    @Test
    fun copyClassAndFunctionTypeParametersIntoStaticFunction() {
        val klass = factory.irClass {
            name = Name.identifier("Box")
            typeParameter("E", builtIns.anyNType)
            thisReceiver()
        }
        val member = klass.irFunction {
            name = Name.identifier("put")
            returnType = builtIns.unitType
            val u = typeParameter("U", klass.typeParameters[0].defaultType)
            valueParameter("u", u.defaultType)
        }

        val static = factory.irFunction {
            copyFrom(member)
            val substitution = copyTypeParametersFrom(member, outer = copyTypeParametersFrom(klass))
            copyParameters(member.parameters, substitution) { from ->
                kind = IrParameterKind.Regular
                if (from.kind == IrParameterKind.DispatchReceiver) name = Name.identifier("\$this")
            }
        }

        val [e, u] = static.typeParameters
        assertEquals(listOf("E", "U"), static.typeParameters.map { it.name.asString() })
        assertEquals(listOf<IrType>(e.defaultType), u.superTypes)
        assertEquals(listOf(IrParameterKind.Regular, IrParameterKind.Regular), static.parameters.map { it.kind })
        assertEquals(listOf("\$this", "u"), static.parameters.map { it.name.asString() })
        assertEquals(klass.symbol.typeWith(e.defaultType), static.parameters[0].type)
        assertEquals(u.defaultType, static.parameters[1].type)
    }
}
