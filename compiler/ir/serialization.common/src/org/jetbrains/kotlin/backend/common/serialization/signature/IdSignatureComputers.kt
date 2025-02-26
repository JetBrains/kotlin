/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isFacadeClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin

class PublicIdSignatureComputer(val mangler: KotlinMangler.IrMangler) : IdSignatureComputer {

    private val publicSignatureBuilder = PublicIdSigBuilder()

    override fun computeSignature(declaration: IrDeclaration): IdSignature {
        return publicSignatureBuilder.buildSignature(declaration)
    }

    fun computePublicIdSignature(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        assert(mangler.run { declaration.isExported(compatibleMode) }) {
            "${declaration.render()} expected to be exported"
        }

        return publicSignatureBuilder.buildSignature(declaration)
    }

    private var currentFileSignatureX: IdSignature.FileSignature? = null

    override fun <R> inFile(file: IrFileSymbol?, block: () -> R): R {
        currentFileSignatureX = file?.let { IdSignature.FileSignature(it) }
        try {
            return block()
        } finally {
            currentFileSignatureX = null
        }
    }

    private fun IrDeclaration.checkIfPlatformSpecificExport(): Boolean = mangler.run { isPlatformSpecificExport() }

    private var localCounter: Long = 0
    private var scopeCounter: Int = 0

    fun reset() {
        localCounter = 0
        scopeCounter = 0
    }

    private inner class PublicIdSigBuilder : IdSignatureBuilder<IrDeclaration, KotlinMangler.IrMangler>() {

        override val mangler: KotlinMangler.IrMangler
            get() = this@PublicIdSignatureComputer.mangler

        override val currentFileSignature: IdSignature.FileSignature?
            get() = currentFileSignatureX

        private val visitor = object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) =
                error("Unexpected element ${element.render()}")

            override fun visitPackageFragment(declaration: IrPackageFragment) {
                packageFqn = declaration.packageFqName
            }

            private val IrDeclarationWithVisibility.isTopLevelPrivate: Boolean
                get() = visibility == DescriptorVisibilities.PRIVATE && !checkIfPlatformSpecificExport() &&
                        (parent is IrPackageFragment || parent.isFacadeClass)

            override fun visitClass(declaration: IrClass) {
                collectParents(declaration)
                isTopLevelPrivate = isTopLevelPrivate || declaration.isTopLevelPrivate
                if (declaration.kind == ClassKind.ENUM_ENTRY) {
                    classFqnSegments.add(MangleConstant.ENUM_ENTRY_CLASS_NAME)
                }
                setDescriptionIfLocalDeclaration(declaration)
                setExpected(declaration.isExpect)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                val property = declaration.correspondingPropertySymbol
                if (property != null) {
                    property.owner.acceptVoid(this)
                    if (container != null) {
                        createContainer()
                    }
                    setHashIdAndDescriptionFor(declaration, isPropertyAccessor = container == null)
                    classFqnSegments.add(declaration.name.asString())
                } else {
                    collectParents(declaration)
                    isTopLevelPrivate = isTopLevelPrivate || declaration.isTopLevelPrivate
                    setHashIdAndDescriptionFor(declaration, isPropertyAccessor = false)

                    // If this is a local function, overwrite `description` with the IR function's rendered form.
                    setDescriptionIfLocalDeclaration(declaration)
                }
                setExpected(declaration.isExpect)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                collectParents(declaration)
                setHashIdAndDescriptionFor(declaration, isPropertyAccessor = false)
                setExpected(declaration.isExpect)
            }

            override fun visitScript(declaration: IrScript) {
                collectParents(declaration)
            }

            override fun visitProperty(declaration: IrProperty) {
                collectParents(declaration)
                isTopLevelPrivate = isTopLevelPrivate || declaration.isTopLevelPrivate
                setHashIdAndDescriptionFor(declaration, isPropertyAccessor = false)
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

                if (parent is IrProperty && parent.setter == rawParent) {
                    classFqnSegments.add(MangleConstant.TYPE_PARAMETER_MARKER_NAME_SETTER)
                } else {
                    classFqnSegments.add(MangleConstant.TYPE_PARAMETER_MARKER_NAME)
                }
                setHashIdAndDescription(declaration.index.toLong(), renderDeclarationForDescription(declaration), isPropertyAccessor = false)
            }

            override fun visitField(declaration: IrField) {
                val prop = declaration.correspondingPropertySymbol?.owner

                if (prop != null) {
                    // backing field
                    prop.acceptVoid(this)
                    createContainer()
                    classFqnSegments.add(MangleConstant.BACKING_FIELD_NAME)
                    setDescriptionIfLocalDeclaration(declaration)
                } else {
                    collectParents(declaration)
                    setHashIdAndDescriptionFor(declaration, isPropertyAccessor = false)
                }
            }
        }

        override fun accept(d: IrDeclaration) {
            d.acceptVoid(visitor)
        }

        private fun collectParents(declaration: IrDeclarationWithName) {
            declaration.parent.acceptVoid(visitor)
            if (declaration !is IrClass || !declaration.isFacadeClass) {
                classFqnSegments.add(declaration.name.asString())
            }
        }

        override fun renderDeclarationForDescription(declaration: IrDeclaration): String = declaration.render()
    }
}

class FileLocalIdSignatureComputer(
    val mangler: KotlinMangler.IrMangler,
    private val signatureByDeclaration: (declaration: IrDeclaration, compatibleMode: Boolean) -> IdSignature,
) {
    private var localIndex: Long = START_INDEX.toLong()
    private var scopeIndex: Int = START_INDEX

    private fun computeContainerIdSignature(
        declaration: IrDeclaration,
        compatibleMode: Boolean,
    ): IdSignature {
        val correspondingPropertySymbol: IrPropertySymbol? = when (declaration) {
            is IrSimpleFunction -> declaration.correspondingPropertySymbol
            is IrField -> declaration.correspondingPropertySymbol
            else -> null
        }

        if (correspondingPropertySymbol != null)
            return signatureByDeclaration(correspondingPropertySymbol.owner, compatibleMode)

        return when (val container = declaration.parent) {
            is IrPackageFragment -> IdSignature.CommonSignature(
                packageFqName = container.packageFqName.asString(),
                declarationFqName = "",
                id = null,
                mask = 0,
                description = null,
            )
            is IrDeclaration -> signatureByDeclaration(container, compatibleMode)
            else -> error("Unexpected container ${container.render()}")
        }
    }

    fun computeFileLocalIdSignature(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature = when (declaration) {
        is IrValueDeclaration -> generateScopeLocalSignature(declaration.name.asString())
        is IrAnonymousInitializer -> generateScopeLocalSignature("ANON INIT")
        is IrLocalDelegatedProperty -> generateScopeLocalSignature(declaration.name.asString())
        is IrOverridableDeclaration<*> if (declaration.isFakeOverride) -> {
            val fakeOverriddenInClass = declaration.parent as IrClass
            val oldFormat = run {
                val file = fakeOverriddenInClass.getPackageFragment() as? IrFile ?: return@run false
                val klibOrigin = file.module.descriptor.getCapability(KlibModuleOrigin.CAPABILITY) as? DeserializedKlibModuleOrigin ?: return@run false
                val version = klibOrigin.library.versions.abiVersion ?: return@run false
                !version.isAtLeast(2, 2, 0)
                        || klibOrigin.library.versions.compilerVersion == "2.2.0-dev-4719"
            }

            if (oldFormat) {
                // Prior to kotlin 2.2.0, local fake overrides used FileLocalSignature, like other local declarations, but with id
                // based on the declaration's name, instead of the location in a file.
                // This was the solution to have a stable signature, that at the same time is predictable by the fake override builder,
                // which does not know anything about locations in the IR file.
                // It was changed to use a dedicated LocalFakeOverrideSignature (KT-72296), but we still need to create signatures
                // in the old format for declarations in the fake override builder, so that they will match with deserialized signatures
                // at call-site, if the call-site comes from an older KLib.
                // Fortunately, since this is a local declaration, it and all its call-sites must have been compiled within the same
                // compilation, and so with the same ABI version.
                IdSignature.FileLocalSignature(
                    container = computeContainerIdSignature(declaration, compatibleMode),
                    id = mangler.run { declaration.signatureMangle(compatibleMode) },
                )
            } else {
                LocalFakeOverrideMangler.run {
                    val mangledName = declaration.signatureString(compatibleMode = false)
                    IdSignature.LocalFakeOverrideSignature(
                        containingClass = signatureByDeclaration(fakeOverriddenInClass, compatibleMode),
                        id = mangledName.hashMangle,
                        mask = 0,
                        description = mangledName,
                    )
                }
            }
        }
        else -> IdSignature.FileLocalSignature(
            container = computeContainerIdSignature(declaration, compatibleMode),
            id = ++localIndex,
            description = declaration.render()
        )
    }

    fun generateScopeLocalSignature(description: String): IdSignature =
        IdSignature.ScopeLocalDeclaration(scopeIndex++, description)

    private object LocalFakeOverrideMangler : IrBasedKotlinManglerImpl() {
        override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> {
            return object : KotlinExportChecker<IrDeclaration> {
                override fun check(declaration: IrDeclaration, type: SpecialDeclarationType): Boolean = false
                override fun IrDeclaration.isPlatformSpecificExported(): Boolean = false
            }
        }

        override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean) =
            object : IrMangleComputer(
                StringBuilder(100),
                MangleMode.SIGNATURE,
                compatibleMode = false,
                allowOutOfScopeTypeParameters = true,
            ) {
                // Avoid including parent (the containing class) in the signature's id.
                // It will be pointed to in the [LocalFakeOverrideSignature.containingClass] field instead.
                override fun IrDeclaration.visitParent() {}
                override fun IrDeclaration.visitParentForFunctionMangling() {}
            }
    }

    companion object {
        private const val START_INDEX = 0
    }
}
