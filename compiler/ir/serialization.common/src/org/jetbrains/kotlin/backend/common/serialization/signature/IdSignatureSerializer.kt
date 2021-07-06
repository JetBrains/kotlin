/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.overrides.isOverridableFunction
import org.jetbrains.kotlin.ir.overrides.isOverridableProperty
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class PublicIdSignatureComputer(val mangler: KotlinMangler.IrMangler) : IdSignatureComputer {

    private val publicSignatureBuilder = PublicIdSigBuilder()

    override fun computeSignature(declaration: IrDeclaration): IdSignature? {
        return if (mangler.run { declaration.isExported() }) {
            publicSignatureBuilder.buildSignature(declaration)
        } else null
    }

    fun composePublicIdSignature(declaration: IrDeclaration): IdSignature {
        assert(mangler.run { declaration.isExported() }) {
            "${declaration.render()} expected to be exported"
        }

        return publicSignatureBuilder.buildSignature(declaration)
    }

    private inner class PublicIdSigBuilder : IdSignatureBuilder<IrDeclaration>(), IrElementVisitorVoid {

        override fun accept(d: IrDeclaration) {
            d.acceptVoid(this)
        }

        private fun collectFqNames(declaration: IrDeclarationWithName) {
            declaration.parent.acceptVoid(this)
            classFqnSegments.add(declaration.name.asString())
        }

        override fun visitElement(element: IrElement) = error("Unexpected element ${element.render()}")

        override fun visitPackageFragment(declaration: IrPackageFragment) {
            packageFqn = declaration.fqName
        }

        override fun visitClass(declaration: IrClass) {
            collectFqNames(declaration)
            setExpected(declaration.isExpect)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            val property = declaration.correspondingPropertySymbol
            if (property != null) {
                hashIdAcc = mangler.run { declaration.signatureMangle }
                property.owner.acceptVoid(this)
                classFqnSegments.add(declaration.name.asString())
            } else {
                hashId = mangler.run { declaration.signatureMangle }
                collectFqNames(declaration)
            }
            setExpected(declaration.isExpect)
        }

        override fun visitConstructor(declaration: IrConstructor) {
            hashId = mangler.run { declaration.signatureMangle }
            collectFqNames(declaration)
            setExpected(declaration.isExpect)
        }

        override fun visitProperty(declaration: IrProperty) {
            hashId = mangler.run { declaration.signatureMangle }
            collectFqNames(declaration)
            setExpected(declaration.isExpect)
        }

        override fun visitTypeAlias(declaration: IrTypeAlias) {
            collectFqNames(declaration)
        }

        override fun visitEnumEntry(declaration: IrEnumEntry) {
            collectFqNames(declaration)
        }
    }
}

open class IdSignatureSerializer(
    private val publicSignatureBuilder: PublicIdSignatureComputer,
    private val table: DeclarationTable,
) : IdSignatureComputer {

    private val mangler: KotlinMangler.IrMangler = publicSignatureBuilder.mangler

    override fun computeSignature(declaration: IrDeclaration): IdSignature? {
        return if (mangler.run { declaration.isExported() }) {
            publicSignatureBuilder.composePublicIdSignature(declaration)
        } else null
    }

    fun composeSignatureForDeclaration(declaration: IrDeclaration): IdSignature {
        return if (mangler.run { declaration.isExported() }) {
            publicSignatureBuilder.composePublicIdSignature(declaration)
        } else composeFileLocalIdSignature(declaration)
    }

    protected var localIndex: Long = 0
    protected var scopeIndex: Int = 0

    private fun composeContainerIdSignature(container: IrDeclarationParent): IdSignature =
        when (container) {
            is IrPackageFragment -> IdSignature.PublicSignature(container.fqName.asString(), "", null, 0)
            is IrDeclaration -> table.signatureByDeclaration(container)
            else -> error("Unexpected container ${container.render()}")
        }

    fun composeFileLocalIdSignature(declaration: IrDeclaration): IdSignature {
        assert(!mangler.run { declaration.isExported() })

        return table.privateDeclarationSignature(declaration) {
            when (declaration) {
                is IrValueDeclaration -> declaration.createScopeLocalSignature(scopeIndex++, declaration.name.asString())
                is IrField -> {
                    val p = declaration.correspondingPropertySymbol?.let { composeSignatureForDeclaration(it.owner) }
                        ?: composeContainerIdSignature(declaration.parent)
                    declaration.createFileLocalSignature(p, ++localIndex)
                }
                is IrSimpleFunction -> {
                    val parent = declaration.parent
                    val p = declaration.correspondingPropertySymbol?.let { composeSignatureForDeclaration(it.owner) }
                        ?: composeContainerIdSignature(parent)
                    declaration.createFileLocalSignature(
                        p,
                        if (declaration.isOverridableFunction()) {
                            mangler.run { declaration.signatureMangle }
                        } else {
                            ++localIndex
                        },
                    )
                }
                is IrProperty -> {
                    val parent = declaration.parent
                    declaration.createFileLocalSignature(
                        composeContainerIdSignature(parent),
                        if (declaration.isOverridableProperty()) {
                            mangler.run { declaration.signatureMangle }
                        } else {
                            ++localIndex
                        },
                    )
                }
                else -> declaration.createFileLocalSignature(composeContainerIdSignature(declaration.parent), ++localIndex)
            }
        }
    }

    protected open fun IrDeclaration.createFileLocalSignature(parentSignature: IdSignature, localIndex: Long): IdSignature {
        return IdSignature.FileLocalSignature(parentSignature, localIndex)
    }

    protected open fun IrDeclaration.createScopeLocalSignature(scopeIndex: Int, description: String): IdSignature {
        return IdSignature.ScopeLocalDeclaration(scopeIndex, description)
    }
}
