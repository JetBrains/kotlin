/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.decompiler.stub.COMPILED_DEFAULT_INITIALIZER
import org.jetbrains.kotlin.analysis.decompiler.stub.COMPILED_DEFAULT_PARAMETER_VALUE
import org.jetbrains.kotlin.analysis.internal.utils.buildIndentedText
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.psi.stubs.KotlinFileStubKind
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

private const val DECOMPILED_CODE_COMMENT = "/* compiled code */"
private const val FLEXIBLE_TYPE_COMMENT = "/* platform type */"
private const val DECOMPILED_CONTRACT_STUB = "contract { /* compiled contract */ }"

@OptIn(IntellijInternalApi::class, KtImplementationDetail::class, KtExperimentalApi::class)
internal fun buildDecompiledText(fileStub: KotlinFileStubImpl): String = buildIndentedText {
    (fileStub.kind as? KotlinFileStubKind.Invalid)?.errorMessage?.let {
        return it
    }

    appendLine("// IntelliJ API Decompiler stub source generated from a class file")
    appendLine("// Implementation of methods is not available")
    appendLine()

    val packageFqName = fileStub.getPackageFqName()
    if (!packageFqName.isRoot) {
        append("package ")
        appendLine(packageFqName.render())
        appendLine()
    }

    // The visitor is declared as local to capture the pretty printer as a context
    val visitor = object : KtVisitorVoid() {
        private fun process(element: PsiElement?) {
            element?.accept(this)
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            withSuffix(" ") { process(classOrObject.modifierList) }
            when (classOrObject) {
                is KtObjectDeclaration -> append("object")
                is KtClass -> when {
                    classOrObject.isInterface() -> append("interface")
                    else -> append("class")
                }
            }

            withPrefix(" ") {
                val name = classOrObject.name?.quoteIfNeeded()
                if (classOrObject !is KtObjectDeclaration || !classOrObject.isCompanion() || name != "Companion") {
                    append(name)
                }
            }

            classOrObject.typeParameterList?.accept(this)
            withPrefix(" ") { process(classOrObject.primaryConstructor) }
            withPrefix(" : ") {
                process(classOrObject.getSuperTypeList())
            }

            withPrefix(" ") { process(classOrObject.typeConstraintList) }
            appendLine(" {")
            withIndent {
                val isEnumClass = classOrObject is KtClass && classOrObject.isEnum()
                val declarationsAndCompanionBlocks = classOrObject.body?.declarationsAndCompanionBlocks.orEmpty()
                val (enumEntries, members) = if (isEnumClass) {
                    declarationsAndCompanionBlocks.partition { it is KtEnumEntry }
                } else {
                    emptyList<KtDeclaration>() to declarationsAndCompanionBlocks
                }

                withSuffix("\n") {
                    appendBlocks(
                        "\n\n",
                        {
                            if (isEnumClass) {
                                appendCollection(enumEntries, separator = ",\n\n", postfix = ";") { process(it) }
                            }
                        },
                        {
                            appendCollection(members, separator = "\n\n", skipIfEmpty = true) { process(it) }
                        }
                    )
                }
            }
            append('}')
        }

        override fun visitCompanionBlock(companionBlock: KtCompanionBlock) {
            appendLine("companion {")
            withIndent {
                withSuffix("\n") {
                    appendCollection(companionBlock.declarations, separator = "\n\n", skipIfEmpty = true) { process(it) }
                }
            }

            append("}")
        }

        override fun visitEnumEntry(enumEntry: KtEnumEntry) {
            withSuffix(" ") { process(enumEntry.modifierList) }
            append(enumEntry.name?.quoteIfNeeded())
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            withSuffix(" ") { process(function.modifierList) }
            append("fun ")
            withSuffix(" ") { process(function.typeParameterList) }
            withSuffix(".") {
                function.receiverTypeReference?.let {
                    printTypeReference(it, position = TypeReferencePosition.DECLARATION_RECEIVER)
                }
            }

            append(function.name?.quoteIfNeeded())
            function.valueParameterList?.accept(this)
            withPrefix(": ") { process(function.typeReference) }

            withPrefix(" ") { process(function.typeConstraintList) }

            printBody(function)
        }

        fun printBody(declaration: KtDeclarationWithBody) {
            if (!declaration.hasBody()) {
                return
            }

            append(" { ")
            if (declaration.mayHaveContract()) {
                append(DECOMPILED_CONTRACT_STUB)
                append("; ")
            }

            append(DECOMPILED_CODE_COMMENT)
            append(" }")
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias) {
            withSuffix(" ") { process(typeAlias.modifierList) }
            append("typealias ")
            append(typeAlias.name?.quoteIfNeeded())
            typeAlias.typeParameterList?.accept(this)
            withPrefix(" ") { process(typeAlias.typeConstraintList) }
            withPrefix(" = ") { process(typeAlias.getTypeReference()) }
        }

        override fun visitConstructor(constructor: KtConstructor<*>) {
            withSuffix(" ") { process(constructor.modifierList) }
            append("constructor")
            constructor.valueParameterList?.accept(this)
            if (constructor is KtSecondaryConstructor) {
                append(" { ")
                append(DECOMPILED_CODE_COMMENT)
                append(" }")
            }
        }

        override fun visitTypeParameter(parameter: KtTypeParameter) {
            withSuffix(" ") { process(parameter.modifierList) }
            append(parameter.name?.quoteIfNeeded())
            withPrefix(" : ") { process(parameter.extendsBound) }
        }

        override fun visitTypeReference(typeReference: KtTypeReference) {
            printTypeReference(typeReference, position = TypeReferencePosition.REGULAR)
        }

        fun printTypeReference(typeReference: KtTypeReference, position: TypeReferencePosition) {
            val modifierList = typeReference.modifierList
            val typeElement = typeReference.typeElement
            val closeParenthesisIsRequired = when (position) {
                TypeReferencePosition.REGULAR -> {
                    withSuffix(" ") { process(modifierList) }
                    false
                }

                TypeReferencePosition.CONTEXT_RECEIVER -> {
                    val openParenthesisIsAdded = modifierList != null && hasPrinted {
                        withPrefix("(") {
                            withSuffix(" ") {
                                printAnnotations(modifierList)
                            }
                        }
                    }

                    withSuffix(" ") {
                        modifierList?.let { printModifiers(it) }
                    }

                    openParenthesisIsAdded
                }

                TypeReferencePosition.DECLARATION_RECEIVER -> {
                    val annotationCallAmbiguityIsImpossible = typeElement is KtUserType ||
                            typeElement is KtNullableType && typeElement.innerType is KtUserType && typeElement.modifierList == null

                    val parenthesisIsAddedWithModifier = hasPrinted {
                        withSuffix(" ") {
                            withPrefix(if (annotationCallAmbiguityIsImpossible) "" else "(") {
                                process(modifierList)
                            }
                        }
                    } && !annotationCallAmbiguityIsImpossible

                    val parenthesisIsRequired = when (typeElement) {
                        is KtFunctionType, is KtIntersectionType -> true
                        else -> false
                    }

                    if (!parenthesisIsAddedWithModifier && parenthesisIsRequired) {
                        append("(")
                    }

                    parenthesisIsAddedWithModifier || parenthesisIsRequired
                }
                TypeReferencePosition.FUNCTION_TYPE_RECEIVER -> {
                    val hasModifier = hasPrinted {
                        withSuffix(" ") {
                            withPrefix("(") {
                                process(modifierList)
                            }
                        }
                    }

                    val parenthesisIsRequired = when (typeElement) {
                        is KtFunctionType, is KtIntersectionType -> true
                        else -> false
                    }

                    if (!hasModifier && parenthesisIsRequired) {
                        append("(")
                    }

                    hasModifier || parenthesisIsRequired
                }
            }

            printTypeElement(typeElement)
            if (closeParenthesisIsRequired) {
                append(")")
            }
        }

        fun printTypeElement(typeElement: KtTypeElement?, printAbbreviatedType: Boolean = true) {
            when (typeElement) {
                null -> {}
                is KtUserType -> {
                    withSuffix(".") { printTypeElement(typeElement.qualifier) }
                    append(typeElement.referencedName?.quoteIfNeeded())
                    val args = typeElement.typeArguments
                    appendCollection(args, prefix = "<", postfix = ">", skipIfEmpty = true) {
                        appendBlocks(
                            separator = " ",
                            { it.projectionKind.token?.value?.let(::append) },
                            { process(it.typeReference) }
                        )
                    }

                    val stubImpl = typeElement.stub as? KotlinUserTypeStubImpl
                    if (stubImpl?.upperBound != null) {
                        append(' ')
                        append(FLEXIBLE_TYPE_COMMENT)
                    }
                }
                is KtFunctionType -> {
                    withSuffix(" ") {
                        appendCollection(
                            typeElement.contextReceiversTypeReferences,
                            prefix = "context(",
                            postfix = ")",
                            skipIfEmpty = true
                        ) {
                            printTypeReference(it, position = TypeReferencePosition.CONTEXT_RECEIVER)
                        }
                    }

                    withSuffix(".") {
                        typeElement.receiverTypeReference?.let {
                            printTypeReference(it, position = TypeReferencePosition.FUNCTION_TYPE_RECEIVER)
                        }
                    }

                    appendCollection(typeElement.parameters, prefix = "(", postfix = ")") { param ->
                        withSuffix(": ") {
                            param.name?.let(::append)
                        }

                        process(param.typeReference)
                    }

                    typeElement.returnTypeReference?.let { returnType ->
                        append(" -> ")
                        process(returnType)
                    }
                }

                /**
                 * Intersection type can be represented only as two simple [KtUserType]s yet,
                 * but this is a future-proof implementation
                 */
                is KtIntersectionType -> {
                    appendBlocks(
                        separator = " & ",
                        {
                            typeElement.getLeftTypeRef()?.let {
                                printTypeReference(it, position = TypeReferencePosition.FUNCTION_TYPE_RECEIVER)
                            }
                        },
                        {
                            typeElement.getRightTypeRef()?.let {
                                printTypeReference(it, position = TypeReferencePosition.FUNCTION_TYPE_RECEIVER)
                            }
                        },
                    )
                }

                is KtNullableType -> {
                    val openParenthesisIsAdded = hasPrinted {
                        withPrefix("(") {
                            withSuffix(" ") {
                                process(typeElement.modifierList)
                            }
                        }
                    }

                    val innerType = typeElement.innerType
                    val openParenthesisIsRequired = innerType !is KtUserType
                    val closeParenthesisIsRequired = openParenthesisIsAdded || openParenthesisIsRequired
                    if (!openParenthesisIsAdded && openParenthesisIsRequired) {
                        append('(')
                    }

                    printTypeElement(innerType, printAbbreviatedType = false)
                    if (closeParenthesisIsRequired) {
                        append(')')
                    }

                    append('?')
                    printAbbreviatedType(innerType)
                }
                is KtDynamicType -> append("dynamic")
                else -> errorWithAttachment("Unsupported type ${typeElement::class.simpleName}") {
                    withPsiEntry("typeElement", typeElement)
                }
            }

            if (printAbbreviatedType) {
                printAbbreviatedType(typeElement)
            }
        }

        fun printAbbreviatedType(typeElement: KtTypeElement?) {
            val abbreviatedType = when (typeElement) {
                is KtUserType -> (typeElement.stub as? KotlinUserTypeStubImpl)?.abbreviatedType
                is KtFunctionType -> (typeElement.stub as? KotlinFunctionTypeStubImpl)?.abbreviatedType
                else -> null
            }

            abbreviatedType?.let(::printAbbreviatedType)
        }

        fun printAbbreviatedType(type: KotlinTypeBean) {
            append(" /* from: ")
            printKotlinTypeBean(type)
            append(" */")
        }

        fun printFqName(fqName: FqName) {
            if (fqName.isRoot) return

            withSuffix(".") { printFqName(fqName.parent()) }
            append(fqName.shortName().asString().quoteIfNeeded())
        }

        fun printClassId(classId: ClassId) {
            withSuffix(".") { printFqName(classId.packageFqName) }
            printFqName(classId.relativeClassName)
        }

        fun printKotlinTypeBean(bean: KotlinTypeBean) {
            when (bean) {
                is KotlinClassTypeBean -> {
                    printClassId(bean.classId)

                    val arguments = bean.arguments
                    appendCollection(arguments, prefix = "<", postfix = ">", skipIfEmpty = true) { arg ->
                        appendBlocks(
                            separator = " ",
                            { arg.projectionKind.token?.value?.let(::append) },
                            { arg.type?.let(::printKotlinTypeBean) },
                        )
                    }

                    if (bean.nullable) {
                        append("?")
                    }

                    val abbreviatedType = bean.abbreviatedType
                    if (abbreviatedType != null) {
                        printAbbreviatedType(abbreviatedType)
                    }
                }

                is KotlinTypeParameterTypeBean -> {
                    append(bean.typeParameterName.quoteIfNeeded())
                    if (bean.nullable) {
                        append("?")
                    }

                    if (bean.definitelyNotNull) {
                        append(" & Any")
                    }
                }

                is KotlinFlexibleTypeBean -> {
                    printKotlinTypeBean(bean.lowerBound)
                    append(" .. ")
                    printKotlinTypeBean(bean.upperBound)
                }
            }
        }

        override fun visitProperty(property: KtProperty) {
            withSuffix(" ") { process(property.modifierList) }

            if (property.isVar) {
                append("var ")
            } else {
                append("val ")
            }

            withSuffix(" ") { process(property.typeParameterList) }
            withSuffix(".") {
                property.receiverTypeReference?.let {
                    printTypeReference(it, position = TypeReferencePosition.DECLARATION_RECEIVER)
                }
            }

            append(property.name?.quoteIfNeeded())
            withPrefix(": ") { process(property.typeReference) }
            withPrefix(" ") { process(property.typeConstraintList) }

            val hasInitializerOrDelegate = hasPrinted {
                when {
                    property.hasDelegate() -> append(" by ")
                    property.hasInitializer() -> append(" = ")
                }
            }

            if (hasInitializerOrDelegate) {
                append(COMPILED_DEFAULT_INITIALIZER)
            }

            if (!property.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                append(" $DECOMPILED_CODE_COMMENT")
            }

            withIndent {
                appendCollection(property.accessors, prefix = "\n", separator = "\n", skipIfEmpty = true) {
                    process(it)
                }
            }
        }

        override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
            withSuffix(" ") { process(accessor.modifierList) }
            if (accessor.isGetter) {
                append("get")
            } else {
                append("set")
            }

            accessor.parameterList?.accept(this)
            withPrefix(": ") { process(accessor.typeReference) }
            printBody(accessor)
        }

        override fun visitParameterList(list: KtParameterList) {
            appendCollection(list.parameters, prefix = "(", postfix = ")") {
                process(it)
            }
        }

        override fun visitParameter(parameter: KtParameter) {
            withSuffix(" ") { process(parameter.modifierList) }
            append(parameter.name?.quoteIfNeeded())
            append(": ")
            parameter.typeReference?.accept(this)
            if (parameter.hasDefaultValue()) {
                append(" = $COMPILED_DEFAULT_PARAMETER_VALUE")
            }
        }

        override fun visitTypeParameterList(list: KtTypeParameterList) {
            appendCollection(list.parameters, prefix = "<", postfix = ">") {
                process(it)
            }
        }

        override fun visitTypeConstraintList(list: KtTypeConstraintList) {
            append("where ")
            appendCollection(list.constraints, separator = ", ") {
                process(it)
            }
        }

        override fun visitTypeConstraint(constraint: KtTypeConstraint) {
            // There is no need to print annotations as they are prohibited in type constraints

            constraint.subjectTypeParameterName?.accept(this)
            append(" : ")
            constraint.boundTypeReference?.accept(this)
        }

        override fun visitModifierList(list: KtModifierList) {
            appendBlocks(
                separator = " ",
                { process(list.contextParameterList) },
                { printAnnotations(list) },
                { printModifiers(list) },
            )
        }

        override fun visitContextParameterList(contextParameterList: KtContextParameterList) {
            val contextElements = contextParameterList.contextParameters.ifEmpty {
                contextParameterList.contextReceivers()
            }

            appendCollection(contextElements, prefix = "context(", postfix = ")") {
                process(it)
            }
        }

        override fun visitContextReceiver(contextReceiver: KtContextReceiver) {
            contextReceiver.typeReference()?.let {
                printTypeReference(it, position = TypeReferencePosition.CONTEXT_RECEIVER)
            }
        }

        override fun visitReferenceExpression(expression: KtReferenceExpression) {
            if (expression is KtSimpleNameExpression) {
                append(expression.getReferencedName())
            } else {
                visitElement(expression)
            }
        }

        fun printAnnotations(container: KtAnnotationsContainer) {
            appendCollection(container.annotationEntries, separator = " ", skipIfEmpty = true) {
                process(it)
            }
        }

        override fun visitSuperTypeList(list: KtSuperTypeList) {
            appendCollection(list.entries) {
                process(it)
            }
        }

        override fun visitSuperTypeListEntry(specifier: KtSuperTypeListEntry) {
            specifier.typeReference?.accept(this)
        }

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
            append('@')
            annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget()?.let {
                append(it.renderName)
                append(':')
            }

            annotationEntry.typeReference?.accept(this)
        }

        override fun visitElement(element: PsiElement) {
            append("/* !${element::class.simpleName}! */")
            super.visitElement(element)
        }

        fun printModifiers(list: KtModifierList) {
            val stub = list.stub as? KotlinModifierListStubImpl ?: return
            if (!stub.hasAnyModifier()) return

            var hadValue = false
            for (modifier in KtTokens.MODIFIER_KEYWORDS_ARRAY) {
                if (!stub.hasModifier(modifier)) continue
                if (hadValue) {
                    append(" ")
                } else {
                    hadValue = true
                }

                append(modifier.value)
            }
        }
    }

    // Psi for files is not guaranteed to present as it has to be set explicitly (see PsiFileStubImpl)
    // On the other side, declarations build psi on demand, so they can be used directly to simplify the logic
    val declarations = fileStub.getChildrenByType(KtFile.FILE_DECLARATION_TYPES, KtDeclaration.ARRAY_FACTORY).asList()
    appendCollection(declarations, separator = "\n\n", postfix = "\n", skipIfEmpty = true) {
        it.accept(visitor)
    }
}

private enum class TypeReferencePosition {
    REGULAR,
    DECLARATION_RECEIVER,
    FUNCTION_TYPE_RECEIVER,
    CONTEXT_RECEIVER,
    ;
}
