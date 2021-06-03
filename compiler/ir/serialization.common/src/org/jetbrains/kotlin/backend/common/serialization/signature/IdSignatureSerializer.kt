/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.overrides.isOverridableFunction
import org.jetbrains.kotlin.ir.overrides.isOverridableProperty
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

open class IdSignatureSerializer(val mangler: KotlinMangler.IrMangler) : IdSignatureComputer {

    override fun computeSignature(declaration: IrDeclaration): IdSignature {
        return composePublicIdSignature(declaration)
    }

    fun composeSignatureForDeclaration(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        return if (mangler.run { declaration.isExported(compatibleMode) }) {
            composePublicIdSignature(declaration)
        } else composeFileLocalIdSignature(declaration, compatibleMode)
    }

    private var currentFileSignatureX: IdSignature.FileSignature? = null

    override fun inFile(file: IrFileSymbol?, block: () -> Unit) {
        currentFileSignatureX = file?.let { IdSignature.FileSignature(it) }

        block()

        currentFileSignatureX = null
    }

    private fun IrDeclaration.checkIfPlatformSpecificExport(): Boolean = mangler.run { isPlatformSpecificExport() }

    private var localCounter: Long = 0
    private var scopeCounter: Int = 0

    // TODO: we need to disentangle signature construction with declaration tables.
    lateinit var table: DeclarationTable

    fun reset() {
        localCounter = 0
        scopeCounter = 0
    }

    private inner class PublicIdSigBuilder : IdSignatureBuilder<IrDeclaration>(), IrElementVisitorVoid {

        override val currentFileSignature: IdSignature.FileSignature?
            get() = currentFileSignatureX

        override fun accept(d: IrDeclaration) {
            d.acceptVoid(this)
        }

        private fun createContainer() {
            container = container?.let {
                buildContainerSignature(it)
            } ?: build()

            reset(false)
        }

        private fun collectParents(declaration: IrDeclarationWithName) {
            declaration.parent.acceptVoid(this)
            classFqnSegments.add(declaration.name.asString())
        }

        private fun setDescription(declaration: IrDeclaration) {
            if (container != null) {
                description = declaration.render()
            }
        }

        override fun visitElement(element: IrElement) =
            error("Unexpected element ${element.render()}")

        override fun visitErrorDeclaration(declaration: IrErrorDeclaration) {
            description = declaration.render()
        }

        override fun visitPackageFragment(declaration: IrPackageFragment) {
            packageFqn = declaration.fqName
        }

        private val IrDeclarationWithVisibility.isTopLevelPrivate: Boolean
            get() = visibility == DescriptorVisibilities.PRIVATE && !checkIfPlatformSpecificExport() && parent is IrPackageFragment

        override fun visitClass(declaration: IrClass) {
            collectParents(declaration)
            isTopLevelPrivate = isTopLevelPrivate || declaration.isTopLevelPrivate
            if (declaration.kind == ClassKind.ENUM_ENTRY) {
                classFqnSegments.add(MangleConstant.ENUM_ENTRY_CLASS_NAME)
            }
            setDescription(declaration)
            setExpected(declaration.isExpect)
        }

        private fun IrDeclarationWithName.hashId(): Long = mangler.run { signatureMangle() }

        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            val property = declaration.correspondingPropertySymbol
            if (property != null) {
                property.owner.acceptVoid(this)
                val preservedId = declaration.hashId()
                if (container != null) {
                    createContainer()
                    hashId = preservedId
                } else {
                    hashIdAcc = preservedId
                }
                classFqnSegments.add(declaration.name.asString())
            } else {
                collectParents(declaration)
                isTopLevelPrivate = isTopLevelPrivate || declaration.isTopLevelPrivate
                hashId = declaration.hashId()
                setDescription(declaration)
            }
            setExpected(declaration.isExpect)
        }

        override fun visitConstructor(declaration: IrConstructor) {
            collectParents(declaration)
            hashId = declaration.hashId()
            setExpected(declaration.isExpect)
        }

        override fun visitProperty(declaration: IrProperty) {
            collectParents(declaration)
            isTopLevelPrivate = isTopLevelPrivate || declaration.isTopLevelPrivate
            hashId = declaration.hashId()
            setExpected(declaration.isExpect)
        }

        override fun visitTypeAlias(declaration: IrTypeAlias) {
            collectParents(declaration)
            isTopLevelPrivate = isTopLevelPrivate || declaration.isTopLevelPrivate
        }

        override fun visitEnumEntry(declaration: IrEnumEntry) {
            collectParents(declaration)
        }

        override fun visitTypeParameter(declaration: IrTypeParameter) {
            val rawParent = declaration.parent

            val parent = if (rawParent is IrSimpleFunction) {
                rawParent.correspondingPropertySymbol?.owner ?: rawParent
            } else rawParent

            parent.accept(this, null)
            createContainer()

            classFqnSegments.add(MangleConstant.TYPE_PARAMETER_MARKER_NAME)
            hashId = declaration.index.toLong()
            description = declaration.render()
        }

        override fun visitField(declaration: IrField) {
            val prop = declaration.correspondingPropertySymbol?.owner

            if (prop != null) {
                // backing field
                prop.acceptVoid(this)
                createContainer()
                classFqnSegments.add(MangleConstant.BACKING_FIELD_NAME)
                description = declaration.render()
            } else {
                collectParents(declaration)
                hashId = declaration.hashId()
            }
        }
    }

    private fun composeContainerIdSignature(container: IrDeclarationParent, compatibleMode: Boolean): IdSignature =
        when (container) {
            is IrPackageFragment -> IdSignature.CommonSignature(container.fqName.asString(), "", null, 0)
            is IrDeclaration -> table.signatureByDeclaration(container, compatibleMode)
            else -> error("Unexpected container ${container.render()}")
        }

    fun composePublicIdSignature(declaration: IrDeclaration): IdSignature {
        return PublicIdSigBuilder().buildSignature(declaration)
    }

    fun composeFileLocalIdSignature(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        return table.privateDeclarationSignature(declaration, compatibleMode) {
            when (declaration) {
                is IrValueDeclaration -> IdSignature.ScopeLocalDeclaration(scopeCounter++, declaration.name.asString())
                is IrAnonymousInitializer -> IdSignature.ScopeLocalDeclaration(scopeCounter++, "ANON INIT")
                is IrLocalDelegatedProperty -> IdSignature.ScopeLocalDeclaration(scopeCounter++, declaration.name.asString())
                is IrField -> {
                    val p = declaration.correspondingPropertySymbol?.let { composeSignatureForDeclaration(it.owner, true) }
                        ?: composeContainerIdSignature(declaration.parent, compatibleMode)
                    IdSignature.FileLocalSignature(p, ++localCounter)
                }
                is IrSimpleFunction -> {
                    val parent = declaration.parent
                    val p = declaration.correspondingPropertySymbol?.let { composeSignatureForDeclaration(it.owner, true) }
                        ?: composeContainerIdSignature(parent, compatibleMode)
                    IdSignature.FileLocalSignature(
                        p,
                        if (declaration.isOverridableFunction()) {
                            mangler.run { declaration.signatureMangle() }
                        } else {
                            ++localCounter
                        },
                        declaration.render()
                    )
                }
                is IrProperty -> {
                    val parent = declaration.parent
                    IdSignature.FileLocalSignature(
                        composeContainerIdSignature(parent, compatibleMode),

                        if (declaration.isOverridableProperty()) {
                            mangler.run { declaration.signatureMangle() }
                        } else {
                            ++localCounter
                        },
                        declaration.render()
                    )
                }
                else -> {
                    IdSignature.FileLocalSignature(composeContainerIdSignature(declaration.parent, compatibleMode), ++localCounter, declaration.render())
                }
            }
        }
    }
}
