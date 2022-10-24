/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.renderer

import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.components.RendererModifier
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal abstract class FirIdeRendererBase(
    protected val options: KtDeclarationRendererOptions,
    protected val useSiteSession: FirSession,
) {
    protected val typeIdeRenderer: ConeTypeIdeRenderer = ConeTypeIdeRenderer(useSiteSession, options.typeRendererOptions)

    protected fun PrettyPrinter.renderAnnotations(declaration: FirDeclaration) {
        if (RendererModifier.ANNOTATIONS in options.modifiers) {
            val isSingleLineAnnotations = declaration is FirValueParameter || declaration is FirTypeParameter
            renderAnnotations(typeIdeRenderer, declaration.annotations, useSiteSession, isSingleLineAnnotations)
        }
    }

    protected fun renderTypeToString(type: ConeTypeProjection): String {
        return typeIdeRenderer.renderType(type)
    }

    protected fun PrettyPrinter.renderType(type: ConeTypeProjection) {
        append(renderTypeToString(type))
    }

    protected fun PrettyPrinter.renderType(firRef: FirTypeRef, approximate: Boolean = false) {
        val coneType = firRef.coneType
        val approximatedIfNeeded = approximate.ifTrue {
            PublicTypeApproximator.approximateTypeToPublicDenotable(coneType, useSiteSession, approximateLocalTypes = true)
        } ?: coneType
        renderType(approximatedIfNeeded)
    }

    protected fun PrettyPrinter.renderName(declaration: FirDeclaration) {
        if (declaration is FirAnonymousObject) {
            append("<no name provided>")
            return
        }
        val name = when (declaration) {
            is FirRegularClass -> declaration.name
            is FirSimpleFunction -> declaration.name
            is FirProperty -> declaration.name
            is FirValueParameter -> declaration.name
            is FirTypeParameter -> declaration.name
            is FirTypeAlias -> declaration.name
            is FirEnumEntry -> declaration.name
            is FirField -> declaration.name
            else -> TODO("Unexpected declaration ${declaration::class.qualifiedName}")
        }
        append(name.render())
    }

    private fun PrettyPrinter.renderVisibility(declaration: FirMemberDeclaration) {
        if (declaration is FirConstructor && declaration.containingClassLookupTag()?.toFirRegularClassSymbol(useSiteSession)?.isEnumClass == true) {
            return
        }
        val visibility = declaration.visibility
        if (RendererModifier.VISIBILITY !in options.modifiers) return

        val currentVisibility = when (visibility) {
            Visibilities.Local -> Visibilities.Public
            Visibilities.PrivateToThis -> Visibilities.Public
            Visibilities.InvisibleFake -> Visibilities.Public
            Visibilities.Inherited -> Visibilities.Public
            Visibilities.Unknown -> Visibilities.Public
            else -> visibility
        }.applyIf(options.normalizedVisibilities) {
            normalize()
        }

        if (currentVisibility == Visibilities.DEFAULT_VISIBILITY) return
        append(currentVisibility.internalDisplayName)
        append(' ')
    }

    private fun PrettyPrinter.renderModality(memberDeclaration: FirMemberDeclaration) {
        val modality = memberDeclaration.modality ?: return
        if ((memberDeclaration as? FirRegularClass)?.isInterface == true) return
        if (modality == Modality.FINAL) return
        if (memberDeclaration.getContainingClassSymbol(useSiteSession)?.classKind == ClassKind.INTERFACE) return
        if (memberDeclaration.isOverride) return
        renderIf(RendererModifier.MODALITY in options.modifiers, modality.name.toLowerCaseAsciiOnly())
    }


    private fun PrettyPrinter.renderOverride(callableMember: FirMemberDeclaration) {
        if (RendererModifier.OVERRIDE !in options.modifiers) return
        renderIf(callableMember.isOverride || options.forceRenderingOverrideModifier, "override")
    }

    protected fun PrettyPrinter.renderIf(value: Boolean, text: String) {
        if (value) {
            append(text)
            append(" ")
        }
    }

    protected fun PrettyPrinter.renderAnnotationsAndModifiers(declaration: FirMemberDeclaration) {
        if (!options.renderDeclarationHeader) return
        renderAnnotations(declaration)
        renderVisibility(declaration)
        renderOverride(declaration)
        renderModality(declaration)
        renderIf(declaration.isExternal, "external")
        renderIf(RendererModifier.EXPECT in options.modifiers && declaration.isExpect, "expect")
        renderIf(RendererModifier.ACTUAL in options.modifiers && declaration.isActual, "actual")
        renderIf(declaration.isTailRec, "tailrec")
        renderIf(declaration.isConst, "const")
        renderIf(declaration.isInner, "inner")
        renderIf(declaration.isLateInit, "lateinit")
        renderIf(declaration.isSuspend, "suspend")
        renderIf(declaration.isInline, "inline")
        renderIf(declaration.isInfix, "infix")
        renderIf(RendererModifier.OPERATOR in options.modifiers && declaration.isOperator, "operator")
    }

    protected fun PrettyPrinter.renderClassifierKind(classifier: FirDeclaration) {
        when (classifier) {
            is FirTypeAlias -> append("typealias")
            is FirRegularClass ->
                append(if (classifier.isCompanion) "companion object" else classifier.classKind.codeRepresentation)
            is FirAnonymousObject -> append("object")
            is FirEnumEntry -> append("enum entry")
            else ->
                throw AssertionError("Unexpected classifier: $classifier")
        }
        append(' ')
    }


    protected fun PrettyPrinter.renderSuperTypes(klass: FirClass) {
        if (klass.defaultType().isNothing) return

        val supertypes = klass.superTypeRefs.asSequence()
            .applyIf(klass.classKind == ClassKind.ENUM_CLASS) {
                filterNot {
                    it.coneType.classId == StandardClassIds.Enum
                }
            }.applyIf(klass.classKind == ClassKind.ANNOTATION_CLASS) {
                filterNot {
                    it.coneType.classId == StandardClassIds.Annotation
                }
            }.toList()

        if (supertypes.isEmpty() || klass.superTypeRefs.singleOrNull()?.isAny == true) return

        append(": ")
        printCollection(supertypes) {
            renderType(it)
        }
        append(' ')
    }
}

