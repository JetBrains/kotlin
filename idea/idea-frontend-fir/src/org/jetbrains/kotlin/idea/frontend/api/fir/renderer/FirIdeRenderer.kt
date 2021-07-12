/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.renderer

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.idea.asJava.applyIf
import org.jetbrains.kotlin.idea.asJava.ifTrue
import org.jetbrains.kotlin.idea.frontend.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.components.RendererModifier
import org.jetbrains.kotlin.idea.frontend.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class FirIdeRenderer private constructor(
    private var containingDeclaration: FirDeclaration?,
    private val options: KtDeclarationRendererOptions,
    private val session: FirSession
) : FirVisitor<Unit, StringBuilder>() {

    private val typeIdeRenderer: ConeTypeIdeRenderer = ConeTypeIdeRenderer(session, options.typeRendererOptions)

    private fun StringBuilder.renderAnnotations(annotated: FirAnnotatedDeclaration) {
        if (RendererModifier.ANNOTATIONS in options.modifiers) {
            renderAnnotations(typeIdeRenderer, annotated.annotations, session)
        }
    }

    private fun renderType(type: ConeTypeProjection, annotations: List<FirAnnotationCall>? = null): String =
        typeIdeRenderer.renderType(type, annotations)

    private fun renderType(firRef: FirTypeRef, approximate: Boolean = false): String {
        require(firRef is FirResolvedTypeRef)

        val approximatedIfNeeded = approximate.ifTrue {
            PublicTypeApproximator.approximateTypeToPublicDenotable(firRef.coneType, session)
        } ?: firRef.coneType
        val annotations = if (RendererModifier.ANNOTATIONS in options.modifiers) {
            firRef.annotations
        } else {
            null
        }
        return renderType(approximatedIfNeeded, annotations)
    }

    private fun StringBuilder.renderName(declaration: FirDeclaration) {
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
            else -> TODO("Unexpected declaration ${declaration::class.qualifiedName}")
        }
        append(name.render())
    }

    private fun StringBuilder.renderCompanionObjectName(firClass: FirRegularClass) {
        if (firClass.name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
            tabRightBySpace()
            append(firClass.name.render())
        }
    }

    private fun StringBuilder.renderVisibility(visibility: Visibility) {
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
        append(" ")
    }

    private fun StringBuilder.renderModality(modality: Modality, defaultModality: Modality) {
        if (modality == defaultModality) return
        renderModifier(RendererModifier.MODALITY in options.modifiers, modality.name.toLowerCaseAsciiOnly())
    }

    private fun FirMemberDeclaration.implicitModalityWithoutExtensions(containingDeclaration: FirDeclaration?): Modality {
        if (this is FirRegularClass) {
            return if (classKind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
        }
        val containingFirClass = containingDeclaration as? FirRegularClass ?: return Modality.FINAL
        if (this !is FirCallableDeclaration) return Modality.FINAL
        if (isOverride) {
            if (containingFirClass.modality != Modality.FINAL) return Modality.OPEN
        }
        return if (containingFirClass.classKind == ClassKind.INTERFACE && this.visibility != Visibilities.Private) {
            if (this.modality == Modality.ABSTRACT) Modality.ABSTRACT else Modality.OPEN
        } else
            Modality.FINAL
    }

    private fun StringBuilder.renderModalityForCallable(
        callable: FirCallableDeclaration,
        containingDeclaration: FirDeclaration?
    ) {
        val modality = callable.modality ?: return
        val isTopLevel = containingDeclaration == null
        if (!isTopLevel || modality != Modality.FINAL) {
            if (callable.isOverride) return
            renderModality(modality, callable.implicitModalityWithoutExtensions(containingDeclaration))
        }
    }

    private fun StringBuilder.renderOverride(callableMember: FirCallableDeclaration) {
        if (RendererModifier.OVERRIDE !in options.modifiers) return
        renderModifier(callableMember.isOverride || options.forceRenderingOverrideModifier, "override")
    }

    private fun StringBuilder.renderModifier(value: Boolean, modifier: String) {
        if (value) {
            append(modifier)
            append(" ")
        }
    }

    private fun StringBuilder.renderMemberModifiers(declaration: FirMemberDeclaration) {
        renderModifier(declaration.isExternal, "external")
        renderModifier(RendererModifier.EXPECT in options.modifiers && declaration.isExpect, "expect")
        renderModifier(RendererModifier.ACTUAL in options.modifiers && declaration.isActual, "actual")
    }

    private fun StringBuilder.renderAdditionalModifiers(firMember: FirMemberDeclaration) {
        val isOperator =
            firMember.isOperator//TODO make similar to functionDescriptor.overriddenDescriptors.none { it.isOperator }
        val isInfix =
            firMember.isInfix//TODO make similar to functionDescriptor.overriddenDescriptors.none { it.isInfix }

        renderModifier(firMember.isTailRec, "tailrec")
        renderSuspendModifier(firMember)
        renderModifier(firMember.isInline, "inline")
        renderModifier(isInfix, "infix")
        renderModifier(RendererModifier.OPERATOR in options.modifiers && isOperator, "operator")
    }

    private fun StringBuilder.renderSuspendModifier(functionDescriptor: FirMemberDeclaration) {
        renderModifier(functionDescriptor.isSuspend, "suspend")
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: StringBuilder) {
        with(data) {
            appendLine()
            appendTabs()
            append("value-parameter").append(" ")
            renderValueParameter(valueParameter)
        }
    }

    override fun visitProperty(property: FirProperty, data: StringBuilder) = with(data) {
        appendLine()
        appendTabs()
        if (options.renderDeclarationHeader) {
            renderAnnotations(property)
            renderVisibility(property.visibility)
            renderModifier(RendererModifier.CONST in options.modifiers && property.isConst, "const")
            renderMemberModifiers(property)
            renderModalityForCallable(property, containingDeclaration)
            renderOverride(property)
            renderModifier(RendererModifier.LATEINIT in options.modifiers && property.isLateInit, "lateinit")
            renderValVarPrefix(property)
            renderTypeParameters(property.typeParameters, true)
        }
        renderReceiver(property)

        renderName(property)
        append(": ").append(renderType(property.returnTypeRef, approximate = options.approximateTypes))

        renderWhereSuffix(property.typeParameters)

        fun FirPropertyAccessor?.needToRender() = this != null && (hasBody || visibility != property.visibility)
        val needToRenderAccessors = options.renderContainingDeclarations &&
                (property.getter.needToRender() || (property.isVar && property.setter.needToRender()))

        fun FirPropertyAccessor?.render(isGetterByDefault: Boolean) {
            if (this == null) {
                appendLine()
                appendTabs()
                append(if (isGetterByDefault) "get" else "set")
            } else {
                visitPropertyAccessor(this, data)
            }
        }

        if (needToRenderAccessors) {
            underBlockDeclaration(property, withBrackets = false) {
                property.getter.render(isGetterByDefault = true)
                if (property.isVar) property.setter.render(isGetterByDefault = false)
            }
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: StringBuilder) {
        require(containingDeclaration is FirProperty) { "Invalid containing declaration" }
        with(data) {
            appendLine()
            appendTabs()
            if (options.renderDeclarationHeader) {
                renderAnnotations(propertyAccessor)
                renderVisibility(propertyAccessor.visibility)
                renderModalityForCallable(propertyAccessor, containingDeclaration)
                renderMemberModifiers(propertyAccessor)
                renderAdditionalModifiers(propertyAccessor)
            }
            append(if (propertyAccessor.isGetter) "get" else "set")
            if (propertyAccessor.isSetter) {
                append("(value: ")
                val renderedType = propertyAccessor.valueParameters.singleOrNull()?.returnTypeRef?.let { renderType(it) }
                if (renderedType != null) append(renderedType) else append(ConeTypeIdeRenderer.ERROR_TYPE_TEXT)
                append(")")
            } else {
                append("()")
            }
        }

        if (options.renderContainingDeclarations) {
            propertyAccessor.body?.let {
                underBlockDeclaration(propertyAccessor, it, data)
            }
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: StringBuilder) {
        with(data) {
            appendLine()
            appendTabs()
            if (options.renderDeclarationHeader) {
                renderAnnotations(simpleFunction)
                renderVisibility(simpleFunction.visibility)

                renderModalityForCallable(simpleFunction, containingDeclaration)
                renderMemberModifiers(simpleFunction)
                renderOverride(simpleFunction)
                renderAdditionalModifiers(simpleFunction)
                append("fun ")
                renderTypeParameters(simpleFunction.typeParameters, true)
            }
            renderReceiver(simpleFunction)
            renderName(simpleFunction)
            renderValueParameters(simpleFunction.valueParameters)

            val returnType = simpleFunction.returnTypeRef
            if (options.renderUnitReturnType || (!returnType.isUnit)) {
                append(": ")
                append(renderType(returnType, approximate = options.approximateTypes))
            }

            renderWhereSuffix(simpleFunction.typeParameters)
        }

        if (options.renderContainingDeclarations) {
            simpleFunction.body?.let {
                underBlockDeclaration(simpleFunction, it, data)
            }
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: StringBuilder) {
        with(data) {
            appendLine()
            appendTabs()

            if (options.renderDeclarationHeader) {
                renderAnnotations(anonymousObject)
            }
            append(getClassifierKindPrefix(anonymousObject))
            renderSuperTypes(anonymousObject)
        }

        if (options.renderContainingDeclarations) {
            data.underBlockDeclaration(anonymousObject) {
                anonymousObject.declarations.forEach {
                    it.accept(this, data)
                }
            }
        }
    }

    override fun visitConstructor(constructor: FirConstructor, data: StringBuilder) {
        with(data) {
            appendLine()
            appendTabs()
            val containingClass = containingDeclaration
            check(containingClass is FirDeclaration && (containingClass is FirClass || containingClass is FirEnumEntry)) {
                "Invalid renderer containing declaration for constructor"
            }
            if (options.renderDeclarationHeader) {
                renderAnnotations(constructor)
            }
            append("constructor")
            renderValueParameters(constructor.valueParameters)
        }

        if (options.renderContainingDeclarations) {
            constructor.body?.let {
                underBlockDeclaration(constructor, it, data)
            }
        }
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: StringBuilder) {
        data.renderTypeParameter(typeParameter, true)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: StringBuilder) {
        with(data) {
            appendLine()
            appendTabs()

            if (options.renderDeclarationHeader) {
                renderAnnotations(regularClass)
                if (regularClass.classKind != ClassKind.ENUM_ENTRY) {
                    renderVisibility(regularClass.visibility)
                }

                val haveNotModality = regularClass.classKind == ClassKind.INTERFACE && regularClass.modality == Modality.ABSTRACT ||
                        regularClass.classKind.isSingleton && regularClass.modality == Modality.FINAL
                if (!haveNotModality) {
                    regularClass.modality?.let {
                        renderModality(it, regularClass.implicitModalityWithoutExtensions(containingDeclaration))
                    }
                }
                renderMemberModifiers(regularClass)
                renderModifier(RendererModifier.INNER in options.modifiers && regularClass.isInner, "inner")
                renderModifier(RendererModifier.DATA in options.modifiers && regularClass.isData, "data")
                renderModifier(RendererModifier.INLINE in options.modifiers && regularClass.isInline, "inline")
                //TODO renderModifier(data, RendererModifier.VALUE in modifiers && regularClass.isValue, "value")
                renderModifier(RendererModifier.FUN in options.modifiers && regularClass.isFun, "fun")
                append(getClassifierKindPrefix(regularClass))
            }

            if (!regularClass.isCompanion) {
                tabRightBySpace()
                renderName(regularClass)
            } else {
                renderCompanionObjectName(regularClass)
            }

            if (regularClass.classKind == ClassKind.ENUM_ENTRY) return

            val typeParameters = regularClass.typeParameters.filterIsInstance<FirTypeParameter>()
            renderTypeParameterRefs(typeParameters, false)
            renderSuperTypes(regularClass)
            renderWhereSuffix(typeParameters)
        }

        fun FirDeclaration.isDefaultPrimaryConstructor() =
            this is FirConstructor &&
                    isPrimary &&
                    valueParameters.isEmpty() &&
                    !hasBody &&
                    (visibility == Visibilities.DEFAULT_VISIBILITY || regularClass.classKind == ClassKind.OBJECT)

        fun FirDeclaration.skipDeclarationForEnumClass(): Boolean {
            if (this is FirConstructor) return isPrimary && valueParameters.isEmpty()
            if (this !is FirSimpleFunction) return false

            if (name.asString() == "values" && valueParameters.isEmpty()) return true

            if (name.asString() == "valueOf") {
                return valueParameters.count() == 1 && (valueParameters[0].returnTypeRef.coneType).classId == StandardClassIds.String
            }
            return false
        }

        fun renderDeclarationForEnumClass() {
            check(regularClass.isEnumClass)
            val partitioned = regularClass.declarations.partition { it is FirEnumEntry }
            partitioned.first.forEach { enumEntry ->
                check(enumEntry is FirEnumEntry)
                visitEnumEntry(enumEntry, data)
                data.append(",")
            }
            partitioned.second.forEach {
                if (!it.skipDeclarationForEnumClass()) {
                    it.accept(this, data)
                }
            }
        }

        fun renderDeclarationForNotEnumClass() {
            check(!regularClass.isEnumClass)
            regularClass.declarations.forEach {
                if (!it.isDefaultPrimaryConstructor()) {
                    it.accept(this, data)
                }
            }
        }

        if (options.renderContainingDeclarations) {
            data.underBlockDeclaration(regularClass) {
                if (regularClass.isEnumClass) {
                    renderDeclarationForEnumClass()
                } else {
                    renderDeclarationForNotEnumClass()
                }
            }
        }
    }

    private var tabbedString = ""

    private inline fun underTabbedBlock(body: () -> Unit) {
        val oldTabbedString = tabbedString
        tabbedString = " ".repeat(tabbedString.length + 4)
        body()
        tabbedString = oldTabbedString
    }

    private inline fun underContainingDeclaration(firDeclaration: FirDeclaration, body: () -> Unit) {
        val oldContainingDeclaration = containingDeclaration
        containingDeclaration = firDeclaration
        body()
        containingDeclaration = oldContainingDeclaration
    }

    private inline fun StringBuilder.underBlockDeclaration(firDeclaration: FirDeclaration, withBrackets: Boolean = true, body: () -> Unit) {
        val oldLength = length
        if (withBrackets) append(" {")
        val unchangedLength = length

        underContainingDeclaration(firDeclaration) {
            underTabbedBlock(body)
        }

        if (unchangedLength != length) {
            if (withBrackets) {
                appendLine()
                appendTabs()
                append("}")
            }
        } else {
            delete(oldLength, unchangedLength)
        }
    }

    private fun StringBuilder.appendTabs() = append(tabbedString)

    private fun underBlockDeclaration(firDeclaration: FirDeclaration, firBlock: FirBlock, data: StringBuilder) {
        data.underBlockDeclaration(firDeclaration) {
            firBlock.accept(this, data)
        }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: StringBuilder) = with(data) {
        if (options.renderDeclarationHeader) {
            renderAnnotations(typeAlias)
            renderVisibility(typeAlias.visibility)
            renderMemberModifiers(typeAlias)
            append("typealias").append(" ")
        }
        renderName(typeAlias)
        renderTypeParameters(typeAlias.typeParameters, false)
        append(" = ").append(renderType(typeAlias.expandedTypeRef))
        Unit
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: StringBuilder) {
        with(data) {
            appendLine()
            appendTabs()
            renderName(enumEntry)
        }
    }

    override fun visitElement(element: FirElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }

    companion object {
        fun render(
            firDeclaration: FirDeclaration,
            containingDeclaration: FirDeclaration?,
            options: KtDeclarationRendererOptions,
            session: FirSession
        ): String {
            val renderer = FirIdeRenderer(
                containingDeclaration,
                options,
                session,
            )
            return buildString {
                firDeclaration.accept(renderer, this)
            }.trim(' ', '\n', '\t')
        }
    }


    /* TYPE PARAMETERS */
    private fun StringBuilder.renderTypeParameter(typeParameter: FirTypeParameter, topLevel: Boolean) {
        if (topLevel) {
            append("<")
        }

        renderModifier(typeParameter.isReified, "reified")
        val variance = typeParameter.variance.label
        renderModifier(variance.isNotEmpty(), variance)
        renderAnnotations(typeParameter)
        renderName(typeParameter)

        val upperBoundsCount = typeParameter.bounds.size
        if ((upperBoundsCount > 1 && !topLevel) || upperBoundsCount == 1) {
            val upperBound = typeParameter.bounds.first()
            if (!upperBound.isNullableAny) {
                append(" : ").append(renderType(upperBound))
            }
        } else if (topLevel) {
            typeParameter.bounds.filterNot { it.isNullableAny }.forEachIndexed { index, upperBound ->
                val separator = if (index == 0) " : " else " & "
                append(separator)
                append(renderType(upperBound))
            }
        } else {
            // rendered with "where"
        }

        if (topLevel) {
            append(">")
        }
    }

    private fun StringBuilder.renderTypeParameterRefs(typeParameters: List<FirTypeParameterRef>, withSpace: Boolean) =
        renderTypeParameters(typeParameters.map { it.symbol.fir }, withSpace)

    private fun StringBuilder.renderTypeParameters(typeParameters: List<FirTypeParameter>, withSpace: Boolean) {
        if (typeParameters.isNotEmpty()) {
            append("<")
            renderTypeParameterList(typeParameters)
            append(">")
            if (withSpace) {
                append(" ")
            }
        }
    }

    private fun StringBuilder.renderTypeParameterList(typeParameters: List<FirTypeParameter>) {
        val iterator = typeParameters.iterator()
        while (iterator.hasNext()) {
            val firTypeParameter = iterator.next()
            renderTypeParameter(firTypeParameter, false)
            if (iterator.hasNext()) {
                append(", ")
            }
        }
    }

    private fun StringBuilder.renderReceiver(firCallableDeclaration: FirCallableDeclaration) {
        val receiverType = firCallableDeclaration.receiverTypeRef
        if (receiverType != null) {
            if (options.renderDeclarationHeader) {
                renderAnnotations(firCallableDeclaration)
            }

            val needBrackets =
                typeIdeRenderer.shouldRenderAsPrettyFunctionType(receiverType.coneType) && receiverType.isMarkedNullable == true

            val result = renderType(receiverType).applyIf(needBrackets) { "($this)" }

            append(result)
            append(".")
        }
    }

    private fun StringBuilder.renderWhereSuffix(typeParameters: List<FirTypeParameterRef>) {

        val upperBoundStrings = ArrayList<String>(0)

        for (typeParameter in typeParameters) {
            val typeParameterFir = typeParameter.symbol.fir
            typeParameterFir.bounds
                .drop(1) // first parameter is rendered by renderTypeParameter
                .mapTo(upperBoundStrings) { typeParameterFir.name.render() + " : " + renderType(it) }
        }

        if (upperBoundStrings.isNotEmpty()) {
            append(" where ")
            upperBoundStrings.joinTo(this, ", ")
        }
    }

    /* VARIABLES */
    private fun StringBuilder.renderValueParameters(valueParameters: List<FirValueParameter>) {
        append("(")
        valueParameters.forEachIndexed { index, valueParameter ->
            if (index != 0) append(", ")
            renderValueParameter(valueParameter)
        }
        append(")")
    }

    private fun StringBuilder.renderValueParameter(valueParameter: FirValueParameter) {
        if (options.renderDeclarationHeader) {
            renderAnnotations(valueParameter)
        }
        renderModifier(valueParameter.isCrossinline, "crossinline")
        renderModifier(valueParameter.isNoinline, "noinline")
        renderVariable(valueParameter)

        if (options.renderDefaultParameterValue) {
            val withDefaultValue = valueParameter.defaultValue != null //TODO check if default value is inherited
            if (withDefaultValue) {
                append(" = ...")
            }
        }
    }

    private fun StringBuilder.renderValVarPrefix(variable: FirVariable, isInPrimaryConstructor: Boolean = false) {
        if (!isInPrimaryConstructor || variable !is FirValueParameter) {
            append(if (variable.isVar) "var" else "val").append(" ")
        }
    }

    private fun StringBuilder.renderVariable(variable: FirVariable) {
        val typeToRender = variable.returnTypeRef
        val isVarArg = (variable as? FirValueParameter)?.isVararg ?: false
        renderModifier(isVarArg, "vararg")
        renderName(variable)
        append(": ")
        val parameterType = typeToRender.coneType
        if (isVarArg) {
            append(renderType(parameterType.arrayElementType() ?: parameterType, typeToRender.annotations))
        } else {
            append(renderType(typeToRender))
        }
    }

    private fun StringBuilder.renderSuperTypes(klass: FirClass) {

        if (klass.defaultType().isNothing) return

        val supertypes = klass.superTypeRefs.applyIf(klass.classKind == ClassKind.ENUM_CLASS) {
            filterNot {
                (it as? FirResolvedTypeRef)?.coneType?.classId == StandardClassIds.Enum
            }
        }.applyIf(klass.classKind == ClassKind.ANNOTATION_CLASS) {
            filterNot {
                (it as? FirResolvedTypeRef)?.coneType?.classId == StandardClassIds.Annotation
            }
        }

        if (supertypes.isEmpty() || klass.superTypeRefs.singleOrNull()?.let { it.isAny || it.isNullableAny } == true) return

        tabRightBySpace()
        append(": ")
        supertypes.joinTo(this, ", ") { renderType(it) }
    }


    private fun getClassifierKindPrefix(classifier: FirDeclaration): String = when (classifier) {
        is FirTypeAlias -> "typealias"
        is FirRegularClass ->
            if (classifier.isCompanion) {
                "companion object"
            } else {
                when (classifier.classKind) {
                    ClassKind.CLASS -> "class"
                    ClassKind.INTERFACE -> "interface"
                    ClassKind.ENUM_CLASS -> "enum class"
                    ClassKind.OBJECT -> "object"
                    ClassKind.ANNOTATION_CLASS -> "annotation class"
                    ClassKind.ENUM_ENTRY -> "enum entry"
                }
            }
        is FirAnonymousObject -> "object"
        is FirEnumEntry -> "enum entry"
        else ->
            throw AssertionError("Unexpected classifier: $classifier")
    }

    private fun StringBuilder.tabRightBySpace() {
        if (length == 0 || last() != ' ') append(' ')
    }
}
