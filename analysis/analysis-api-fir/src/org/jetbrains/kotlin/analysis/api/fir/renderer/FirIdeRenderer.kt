/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.renderer

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.PsiSourceNavigator.getRawName
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.renderer.render

internal class FirIdeRenderer private constructor(
    options: KtDeclarationRendererOptions,
    session: FirSession,
) : FirIdeRendererBase(options, session) {
    fun PrettyPrinter.renderMemberDeclaration(declaration: FirDeclaration) {
        when (declaration) {
            is FirAnonymousObject -> renderAnonymousObject(declaration)
            is FirRegularClass -> renderRegularClass(declaration)
            is FirTypeAlias -> renderTypeAlias(declaration)
            is FirConstructor -> renderConstructor(declaration)
            is FirPropertyAccessor -> renderPropertyAccessor(declaration)
            is FirSimpleFunction -> renderSimpleFunction(declaration)
            is FirBackingField -> renderBackingField()
            is FirEnumEntry -> renderEnumEntry(declaration)
            is FirProperty -> renderPropertyOrField(declaration)
            is FirValueParameter -> renderValueParameter(declaration)
            is FirField -> renderPropertyOrField(declaration)
            is FirErrorFunction -> error("FirErrorFunction should not be rendered")
            is FirErrorProperty -> error("FirErrorProperty should not be rendered")
            is FirAnonymousInitializer -> error("FirAnonymousInitializer should not be rendered")
            is FirFile ->  error("FirFile should not be rendered")
            is FirTypeParameter -> renderTypeParameter(declaration)
            is FirAnonymousFunction -> TODO()
        }
    }

    private fun PrettyPrinter.renderBackingField() {
        append("field")
    }

    private fun PrettyPrinter.renderPropertyOrField(variable: FirVariable) {
        check(variable is FirProperty || variable is FirField) {
            "Required either FirProperty or FirField but was ${variable::class.simpleName}"
        }
        renderAnnotationsAndModifiers(variable)
        renderValVarPrefix(variable)
        renderTypeParameters(variable)
        renderReceiver(variable)
        renderName(variable)
        append(": ")
        renderType(variable.returnTypeRef, approximate = options.approximateTypes)

        renderWhereSuffix(variable)

        fun FirPropertyAccessor?.needToRender() = this != null && (annotations.isNotEmpty() || visibility != variable.visibility)
        val needToRenderAccessors = options.renderClassMembers &&
                (variable.getter.needToRender() || (variable.isVar && variable.setter.needToRender()))

        if (needToRenderAccessors) {
            withIndent {
                variable.getter?.let { getter ->
                    if (getter.needToRender()) {
                        appendLine()
                        renderPropertyAccessor(getter)
                    }
                }
                variable.setter?.let { setter ->
                    if (setter.needToRender()) {
                        appendLine()
                        renderPropertyAccessor(setter)
                    }
                }
            }
        }
    }

    private fun PrettyPrinter.renderPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        renderAnnotationsAndModifiers(propertyAccessor)
        append(if (propertyAccessor.isGetter) "get" else "set")
        if (propertyAccessor.isSetter) {
            append("(")
            val valueParameter = propertyAccessor.valueParameters.first()
            renderAnnotations(valueParameter)
            append("value: ")
            renderType(valueParameter.returnTypeRef)
            append(")")
        } else {
            append("()")
        }
        renderFunctionBody(propertyAccessor)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun PrettyPrinter.renderFunctionBody(function: FirFunction) {
        // TODO implement with some settings
    }

    private fun PrettyPrinter.renderSimpleFunction(simpleFunction: FirSimpleFunction) {
        renderAnnotationsAndModifiers(simpleFunction)
        append("fun ")
        renderTypeParameters(simpleFunction)
        renderReceiver(simpleFunction)
        renderName(simpleFunction)
        renderValueParameters(simpleFunction)

        val returnType = simpleFunction.returnTypeRef
        if (options.renderUnitReturnType || !returnType.isUnit) {
            append(": ")
            renderType(returnType, approximate = options.approximateTypes)
            append(' ')
        }

        renderWhereSuffix(simpleFunction)
        renderFunctionBody(simpleFunction)
    }

    private fun PrettyPrinter.renderAnonymousObject(anonymousObject: FirAnonymousObject) {
        renderAnnotationsAndModifiers(anonymousObject)
        append("object ")
        renderSuperTypes(anonymousObject)
        renderClassBody(anonymousObject)
    }

    private fun PrettyPrinter.renderClassBody(firClass: FirClass) {
        if (!options.renderClassMembers) return
        if (firClass.declarations.isEmpty()) return

        val allDeclarations = buildList {
            firClass.declarations.filterNotTo(this) { member ->
                member.isDefaultPrimaryConstructor()
                        || member.isDefaultEnumEntryMember(firClass)
                        || member is FirConstructor && firClass.classKind == ClassKind.OBJECT
            }
            addAll(firClass.generatedNestedClassifiers(useSiteSession))
            addAll(firClass.generatedMembers(useSiteSession))
        }.filterIsInstance<FirMemberDeclaration>()
        if (allDeclarations.isEmpty()) return

        val (enumEntries, nonEnumEntries) = allDeclarations.partition { it is FirEnumEntry }
        withIndentInBraces {
            printCollection(sortDeclarations(enumEntries), separator = ",\n") { declaration ->
                renderMemberDeclaration(declaration)
            }

            if (enumEntries.isNotEmpty() && nonEnumEntries.isNotEmpty()) {
                appendLine(";\n")
            }

            printCollection(sortDeclarations(nonEnumEntries), separator = "\n\n") { declaration ->
                renderMemberDeclaration(declaration)
            }
        }
    }

    private fun PrettyPrinter.renderConstructor(constructor: FirConstructor) {
        renderAnnotationsAndModifiers(constructor)
        append("constructor")
        renderValueParameters(constructor)
        renderFunctionBody(constructor)
    }

    private fun PrettyPrinter.renderRegularClass(regularClass: FirRegularClass) {
        renderAnnotationsAndModifiers(regularClass)
        renderClassifierKind(regularClass)
        renderClassName(regularClass)
        renderTypeParameters(regularClass)
        printCharIfNotThere(' ')
        renderSuperTypes(regularClass)
        renderWhereSuffix(regularClass)
        renderClassBody(regularClass)
    }

    private fun PrettyPrinter.renderClassName(regularClass: FirRegularClass) {
        if (!regularClass.isCompanion) {
            renderName(regularClass)
        } else {
            if (regularClass.name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
                append(regularClass.name.render())
            }
        }
    }

    private fun PrettyPrinter.renderTypeAlias(typeAlias: FirTypeAlias) {
        renderAnnotationsAndModifiers(typeAlias)
        append("typealias ")
        renderName(typeAlias)
        renderTypeParameters(typeAlias)
        printCharIfNotThere(' ')
        append("= ")
        renderType(typeAlias.expandedTypeRef)
    }

    private fun PrettyPrinter.renderEnumEntry(enumEntry: FirEnumEntry) {
        renderName(enumEntry)
    }

    private fun PrettyPrinter.renderTypeParameter(typeParameter: FirTypeParameter) {
        renderIf(typeParameter.isReified, "reified")
        val variance = typeParameter.variance.label
        renderIf(variance.isNotEmpty(), variance)
        renderAnnotations(typeParameter)
        renderName(typeParameter)

        val upperBoundsCount = typeParameter.bounds.size
        if (upperBoundsCount >= 1) {
            val upperBound = typeParameter.bounds.first()
            if (!upperBound.isNullableAny) {
                append(" : ")
                renderType(upperBound)
            }
        }
    }

    private fun PrettyPrinter.renderTypeParameters(declaration: FirMemberDeclaration) {
        val typeParameters = declaration.typeParameters.filterIsInstance<FirTypeParameter>()
        if (typeParameters.isNotEmpty()) {
            append("<")
            printCollection(typeParameters) {
                renderTypeParameter(it)
            }
            append("> ")
        }
    }

    private fun PrettyPrinter.renderReceiver(firCallableDeclaration: FirCallableDeclaration) {
        val receiverType = firCallableDeclaration.receiverTypeRef
        if (receiverType != null) {
            if (options.renderDeclarationHeader) {
                renderAnnotations(firCallableDeclaration)
            }

            val needBrackets =
                typeIdeRenderer.shouldRenderAsPrettyFunctionType(receiverType.coneType) && receiverType.isMarkedNullable == true

            if (needBrackets) append('(')
            renderType(receiverType)
            if (needBrackets) append(')')
            append(".")
        }
    }

    private fun PrettyPrinter.renderWhereSuffix(declaration: FirTypeParameterRefsOwner) {
        val upperBoundStrings = ArrayList<String>(0)

        for (typeParameter in declaration.typeParameters) {
            if (typeParameter !is FirTypeParameter) continue
            typeParameter.symbol.resolvedBounds
                .drop(1) // first parameter is rendered by renderTypeParameter
                .mapTo(upperBoundStrings) { typeParameter.name.render() + " : " + renderTypeToString(it.coneType) }
        }

        if (upperBoundStrings.isNotEmpty()) {
            append("where ")
            upperBoundStrings.joinTo(this, ", ")
            append(' ')
        }
    }

    private fun PrettyPrinter.renderValueParameters(function: FirFunction) {
        printCollection(function.valueParameters, prefix = "(", postfix = ")") {
            renderValueParameter(it)
        }
    }

    private fun PrettyPrinter.renderValueParameter(valueParameter: FirValueParameter) {
        if (options.renderDeclarationHeader) {
            renderAnnotations(valueParameter)
        }
        renderIf(valueParameter.isCrossinline, "crossinline")
        renderIf(valueParameter.isNoinline, "noinline")
        renderVariable(valueParameter)

        if (options.renderDefaultParameterValue) {
            val withDefaultValue = valueParameter.defaultValue != null //TODO check if default value is inherited
            if (withDefaultValue) {
                append(" = ...")
            }
        }
    }

    private fun PrettyPrinter.renderValVarPrefix(variable: FirVariable, isInPrimaryConstructor: Boolean = false) {
        if (!isInPrimaryConstructor || variable !is FirValueParameter) {
            append(if (variable.isVar) "var" else "val")
            append(' ')
        }
    }

    private fun PrettyPrinter.renderVariable(variable: FirVariable) {
        val typeToRender = variable.returnTypeRef.coneType
        val isVarArg = (variable as? FirValueParameter)?.isVararg ?: false
        renderIf(isVarArg, "vararg")
        renderName(variable)
        append(": ")
        if (isVarArg) {
            renderType(typeToRender.arrayElementType() ?: typeToRender)
        } else {
            renderType(typeToRender)
        }
    }

    fun sortDeclarations(declarations: List<FirMemberDeclaration>): List<FirMemberDeclaration> {
        if (!options.sortNestedDeclarations) return declarations

        fun getDeclarationKind(declaration: FirDeclaration): Int = when (declaration) {
            is FirEnumEntry -> 0
            is FirConstructor -> if (declaration.isPrimary) 1 else 2
            is FirProperty -> 3
            is FirFunction -> 4
            else -> 5
        }

        return declarations.sortedWith(Comparator { left, right ->
            val kindResult = getDeclarationKind(left) - getDeclarationKind(right)
            if (kindResult != 0) {
                return@Comparator kindResult
            }

            val nameResult = (left.getRawName() ?: "").compareTo(right.getRawName() ?: "")
            if (nameResult != 0) {
                return@Comparator nameResult
            }

            val leftString = prettyPrint { renderMemberDeclaration(left) }
            val rightString = prettyPrint { renderMemberDeclaration(right) }
            return@Comparator leftString.compareTo(rightString)
        })
    }


    companion object {
        fun render(
            firDeclaration: FirDeclaration,
            options: KtDeclarationRendererOptions,
            session: FirSession
        ): String {
            val renderer = FirIdeRenderer(options, session)
            return prettyPrint {
                with(renderer) { renderMemberDeclaration(firDeclaration) }
            }.trim { it.isWhitespace() }
        }
    }
}

private fun FirDeclaration.isDefaultEnumEntryMember(firClass: FirClass): Boolean {
    if (firClass.classKind != ClassKind.ENUM_CLASS) return false
    if (this is FirConstructor) return isPrimary && valueParameters.isEmpty()
    return source?.kind == KtFakeSourceElementKind.EnumGeneratedDeclaration
}

private fun FirDeclaration.isDefaultPrimaryConstructor() =
    this is FirConstructor &&
            isPrimary &&
            valueParameters.isEmpty() &&
            !hasBody &&
            visibility == Visibilities.DEFAULT_VISIBILITY
