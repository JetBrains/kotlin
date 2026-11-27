/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.serialization.SignatureAndKind
import org.jetbrains.kotlin.backend.common.serialization.signatureAndKind
import org.jetbrains.kotlin.backend.common.serialization.signatureAndKindOrNull
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Checks [SignatureAndKind.topLevelOrNull] computed for the declarations in the IR deserialized from a KLIB:
 * - For a local declaration, there is no top-level [SignatureAndKind].
 * - For any other declaration, it is the [SignatureAndKind] of the top-level declaration that this declaration belongs to.
 *
 * The typical usage of this handler is the following:
 * ```
 * facadeStep(<deserializer>)
 * deserializedIrHandlersStep { useHandlers(::SignatureAndKindHandler) }
 * ```
 */
class SignatureAndKindHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    override fun processModule(module: TestModule, info: IrBackendInput) {
        for (file in info.irModuleFragment.files) {
            for (topLevelDeclaration in file.declarations) {
                // Visit the top-level declaration itself, and then all declarations nested into it.
                topLevelDeclaration.accept(Visitor(topLevelDeclaration), data = State.NON_LOCAL)
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    /** The data passed through the visitor is whether the visited element is located inside a body. */
    private inner class Visitor(topLevelDeclaration: IrDeclaration) : IrVisitor<Unit, State>() {
        private val topLevelSignatureAndKind: SignatureAndKind = topLevelDeclaration.symbol.signatureAndKindOrNull
            ?: assertions.fail { "Cannot compute signature and kind for top-level declaration ${topLevelDeclaration.render()}" }

        override fun visitElement(element: IrElement, data: State) = element.acceptChildren(this, data)

        override fun visitBody(body: IrBody, data: State) = body.acceptChildren(this, data = State.LOCAL)

        override fun visitDeclaration(declaration: IrDeclarationBase, data: State) {
            // Local declarations: Everything that is located inside a body, and also value parameters and anonymous initializers.
            val isLocal = data == State.LOCAL ||
                    declaration is IrValueParameter ||
                    declaration is IrTypeParameter ||
                    declaration is IrAnonymousInitializer

            // Note: The symbol of a local declaration has no signature.
            val signatureAndKind = declaration.symbol.signatureAndKindOrNull
            if (signatureAndKind != null) {
                assertions.assertEquals(
                    expected = if (isLocal) null else topLevelSignatureAndKind,
                    actual = signatureAndKind.topLevelOrNull()
                ) {
                    "Unexpected result of topLevelOrNull() for [$signatureAndKind] of ${declaration.render()}"
                }
            }

            super.visitDeclaration(declaration, data)
        }

        override fun visitClass(declaration: IrClass, data: State) {
            // A local class is not always located inside a body: for example, an inner class declared in an enum entry.
            // Such a class and all its members are checked the same way as declarations inside a body.
            val isLocalClass = declaration.visibility == DescriptorVisibilities.LOCAL
            super.visitClass(declaration, if (isLocalClass) State.LOCAL else data)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: State) {
            super.visitSimpleFunction(declaration, data)

            if (data == State.NON_LOCAL) {
                val propertyIfAccessor = declaration.correspondingPropertySymbol?.owner
                if (propertyIfAccessor != null) {
                    val signatureAndKind = declaration.symbol.signatureAndKind
                    assertions.assertEquals(
                        expected = propertyIfAccessor.symbol.signatureAndKind,
                        actual = signatureAndKind.correspondingPropertyOrNull()
                    ) {
                        "Unexpected result of correspondingPropertyOrNull() for [$signatureAndKind] of ${declaration.render()}"
                    }
                }
            }
        }

        override fun visitField(declaration: IrField, data: State) {
            super.visitField(declaration, data)

            if (data == State.NON_LOCAL) {
                val propertyIfBackingField = declaration.correspondingPropertySymbol?.owner
                if (propertyIfBackingField != null) {
                    val signatureAndKind = declaration.symbol.signatureAndKind
                    assertions.assertEquals(
                        expected = propertyIfBackingField.symbol.signatureAndKind,
                        actual = signatureAndKind.correspondingPropertyOrNull()
                    ) {
                        "Unexpected result of correspondingPropertyOrNull() for [$signatureAndKind] of ${declaration.render()}"
                    }
                }
            }
        }
    }

    private enum class State {
        LOCAL,
        NON_LOCAL,
    }
}

