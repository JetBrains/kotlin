/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.TestIrBuiltins
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for [CustomKotlinLikeDumpStrategy.nameOf] with actual [IrCallImpl] expressions in context.
 *
 * Builds IR representing:
 * ```
 * class InstanceFactory {
 *   companion object {
 *     fun create(): Unit = ...
 *   }
 * }
 *
 * class Example {
 *   fun use() {
 *     InstanceFactory.Companion.create()  // call in context of Example
 *   }
 * }
 * ```
 *
 * When dumping `Example`, the `container` passed to [CustomKotlinLikeDumpStrategy.nameOf] for the call
 * to `create` is the `use` function inside `Example` — enabling context-aware name rendering.
 */
class CustomKotlinLikeDumpStrategyTest {

    private val pkg = IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(), FqName("test"))

    private data class TestIr(
        val instanceFactory: IrClass,
        val example: IrClass,
        val createFun: IrSimpleFunction,
    )

    private fun buildIr(): TestIr {
        val instanceFactory = IrFactoryImpl.buildClass {
            name = Name.identifier("InstanceFactory")
        }.apply {
            parent = pkg
        }

        val companion = IrFactoryImpl.buildClass {
            name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
            isCompanion = true
        }.apply {
            parent = instanceFactory
        }

        val createFun = IrFactoryImpl.buildFun {
            name = Name.identifier("create")
            returnType = TestIrBuiltins.unitType
        }.apply {
            parent = companion
        }

        instanceFactory.declarations += companion
        companion.declarations += createFun

        // Example class with a function whose body calls InstanceFactory.Companion.create()
        val example = IrFactoryImpl.buildClass {
            name = Name.identifier("Example")
        }.apply {
            parent = pkg
        }

        val useFun = IrFactoryImpl.buildFun {
            name = Name.identifier("use")
            returnType = TestIrBuiltins.unitType
        }.apply {
            parent = example
        }

        val call = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.unitType, createFun.symbol)
        useFun.body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
            statements += call
        }

        example.declarations += useFun

        return TestIr(instanceFactory, example, createFun)
    }

    @Test
    fun `default nameOf renders simple function name in call`() {
        val (_, example, _) = buildIr()
        val dump = example.dumpKotlinLike()
        // Default: just the simple name "create()"
        assertEquals(
            """
                class Example {
                  /* static */ fun use() {
                    create()
                  }

                }
                
                
            """.trimIndent(),
            dump.replace("\r\n", "\n"),
        )
    }

    @Test
    fun `fully qualified nameOf renders full parent chain in call`() {
        val (_, example, _) = buildIr()
        val options = KotlinLikeDumpOptions(
            customDumpStrategy = object : CustomKotlinLikeDumpStrategy {
                override fun nameOf(container: IrDeclaration?, declaration: IrDeclarationWithName): String {
                    return qualifiedName(declaration, skipCompanion = false)
                }
            }
        )
        val dump = example.dumpKotlinLike(options)
        assertEquals(
            """
                class Example {
                  /* static */ fun use() {
                    InstanceFactory.Companion.create()
                  }

                }
                
                
            """.trimIndent(),
            dump.replace("\r\n", "\n"),
        )
    }

    @Test
    fun `custom nameOf skips companion segment in call`() {
        val (_, example, _) = buildIr()
        val options = KotlinLikeDumpOptions(
            customDumpStrategy = object : CustomKotlinLikeDumpStrategy {
                override fun nameOf(container: IrDeclaration?, declaration: IrDeclarationWithName): String {
                    return qualifiedName(declaration, skipCompanion = true)
                }
            }
        )
        val dump = example.dumpKotlinLike(options)
        // "Companion" should be stripped from the chain
        assertEquals(
            """
                class Example {
                  /* static */ fun use() {
                    InstanceFactory.create()
                  }

                }
                
                
            """.trimIndent(),
            dump.replace("\r\n", "\n"),
        )
    }

    companion object {
        private fun qualifiedName(declaration: IrDeclarationWithName, skipCompanion: Boolean): String {
            val parts = mutableListOf<String>()
            var current: IrDeclarationWithName? = declaration
            while (current != null) {
                if (!(skipCompanion && current.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)) {
                    parts.add(0, current.name.asString())
                }
                current = (current as? IrDeclaration)?.parentClassOrNull
            }
            return parts.joinToString(".")
        }
    }
}
