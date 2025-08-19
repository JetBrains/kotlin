/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.decompiler.stub.COMPILED_DEFAULT_INITIALIZER
import org.jetbrains.kotlin.analysis.decompiler.stub.COMPILED_DEFAULT_PARAMETER_VALUE
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.psi.stubs.KotlinFileStubKind
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

private const val DECOMPILED_CODE_COMMENT = "/* compiled code */"
private const val FLEXIBLE_TYPE_COMMENT = "/* platform type */"
private const val DECOMPILED_CONTRACT_STUB = "contract { /* compiled contract */ }"

@OptIn(IntellijInternalApi::class, KtImplementationDetail::class)
internal fun buildDecompiledText(fileStub: KotlinFileStubImpl): String = PrettyPrinter(indentSize = 4).apply {
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
        // A workaround to access the object in a nested context
        private inline val explicitThis get() = this

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            withSuffix(" ") { classOrObject.modifierList?.accept(this) }
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
            withPrefix(" ") { classOrObject.primaryConstructor?.accept(this) }
            withPrefix(" : ") {
                classOrObject.getSuperTypeList()?.accept(this)
            }

            withPrefix(" ") { classOrObject.typeConstraintList?.accept(this) }
            appendLine(" {")
            withIndent {
                val isEnumClass = classOrObject is KtClass && classOrObject.isEnum()
                val (enumEntries, members) = if (isEnumClass) {
                    classOrObject.declarations.partition { it is KtEnumEntry }
                } else {
                    emptyList<KtDeclaration>() to classOrObject.declarations
                }

                withSuffix("\n") {
                    "\n\n".separated(
                        {
                            if (isEnumClass) {
                                printCollection(enumEntries, separator = ",\n\n", postfix = ";") { it.accept(explicitThis) }
                            }
                        },
                        { printCollectionIfNotEmpty(members, separator = "\n\n") { it.accept(explicitThis) } },
                    )
                }
            }
            append('}')
        }

        override fun visitEnumEntry(enumEntry: KtEnumEntry) {
            withSuffix(" ") { enumEntry.modifierList?.accept(this) }
            append(enumEntry.name?.quoteIfNeeded())
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            withSuffix(" ") { function.modifierList?.accept(this) }
            append("fun ")
            withSuffix(" ") { function.typeParameterList?.accept(this) }
            withSuffix(".") {
                function.receiverTypeReference?.let {
                    printTypeReference(it, position = TypeReferencePosition.DECLARATION_RECEIVER)
                }
            }

            append(function.name?.quoteIfNeeded())
            function.valueParameterList?.accept(this)
            withPrefix(": ") { function.typeReference?.accept(this) }

            withPrefix(" ") { function.typeConstraintList?.accept(this) }

            if (function.hasBody()) {
                append(" { ")
                if (function.mayHaveContract()) {
                    append(DECOMPILED_CONTRACT_STUB)
                    append("; ")
                }
                append(DECOMPILED_CODE_COMMENT)
                append(" }")
            }
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias) {
            withSuffix(" ") { typeAlias.modifierList?.accept(this) }
            append("typealias ")
            append(typeAlias.name?.quoteIfNeeded())
            typeAlias.typeParameterList?.accept(this)
            withPrefix(" ") { typeAlias.typeConstraintList?.accept(this) }
            withPrefix(" = ") { typeAlias.getTypeReference()?.accept(this) }
        }

        override fun visitConstructor(constructor: KtConstructor<*>) {
            withSuffix(" ") { constructor.modifierList?.accept(this) }
            append("constructor")
            constructor.valueParameterList?.accept(this)
            if (constructor is KtSecondaryConstructor) {
                append(" { ")
                append(DECOMPILED_CODE_COMMENT)
                append(" }")
            }
        }

        override fun visitTypeParameter(parameter: KtTypeParameter) {
            withSuffix(" ") { parameter.modifierList?.accept(this) }
            append(parameter.name?.quoteIfNeeded())
            withPrefix(" : ") { parameter.extendsBound?.accept(this) }
        }

        override fun visitTypeReference(typeReference: KtTypeReference) {
            printTypeReference(typeReference, position = TypeReferencePosition.REGULAR)
        }

        fun printTypeReference(typeReference: KtTypeReference, position: TypeReferencePosition) {
            val modifierList = typeReference.modifierList
            val typeElement = typeReference.typeElement
            val closeParenthesisIsRequired = when (position) {
                TypeReferencePosition.REGULAR -> {
                    withSuffix(" ") { modifierList?.accept(this) }
                    false
                }

                TypeReferencePosition.CONTEXT_RECEIVER -> {
                    val openParenthesisIsAdded = modifierList != null && checkIfPrinted {
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

                    val parenthesisIsAddedWithModifier = checkIfPrinted {
                        withSuffix(" ") {
                            withPrefix(if (annotationCallAmbiguityIsImpossible) "" else "(") {
                                modifierList?.accept(this)
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
                    val hasModifier = checkIfPrinted {
                        withSuffix(" ") {
                            withPrefix("(") {
                                modifierList?.accept(this)
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
                    printCollectionIfNotEmpty(args, prefix = "<", postfix = ">") {
                        " ".separated(
                            { it.projectionKind.token?.value?.let(::append) },
                            { it.typeReference?.accept(explicitThis) }
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
                        printCollectionIfNotEmpty(typeElement.contextReceiversTypeReferences, prefix = "context(", postfix = ")") {
                            printTypeReference(it, position = TypeReferencePosition.CONTEXT_RECEIVER)
                        }
                    }

                    withSuffix(".") {
                        typeElement.receiverTypeReference?.let {
                            printTypeReference(it, position = TypeReferencePosition.FUNCTION_TYPE_RECEIVER)
                        }
                    }

                    printCollection(typeElement.parameters, prefix = "(", postfix = ")") { param ->
                        withSuffix(": ") {
                            param.name?.let(::append)
                        }

                        param.typeReference?.accept(explicitThis)
                    }

                    typeElement.returnTypeReference?.let { returnType ->
                        append(" -> ")
                        returnType.accept(explicitThis)
                    }
                }

                /**
                 * Intersection type can be represented only as two simple [KtUserType]s yet,
                 * but this is a future-proof implementation
                 */
                is KtIntersectionType -> {
                    " & ".separated(
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
                    val openParenthesisIsAdded = checkIfPrinted {
                        withPrefix("(") {
                            withSuffix(" ") {
                                typeElement.modifierList?.accept(this)
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
                    printCollectionIfNotEmpty(arguments, prefix = "<", postfix = ">") { arg ->
                        " ".separated(
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
            withSuffix(" ") { property.modifierList?.accept(this) }

            if (property.isVar) {
                append("var ")
            } else {
                append("val ")
            }

            withSuffix(" ") { property.typeParameterList?.accept(this) }
            withSuffix(".") {
                property.receiverTypeReference?.let {
                    printTypeReference(it, position = TypeReferencePosition.DECLARATION_RECEIVER)
                }
            }

            append(property.name?.quoteIfNeeded())
            withPrefix(": ") { property.typeReference?.accept(this) }
            withPrefix(" ") { property.typeConstraintList?.accept(this) }

            val hasInitializerOrDelegate = checkIfPrinted {
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

            property.stub?.hasBackingField?.let {
                append(' ')
                append(StubUtils.HAS_BACKING_FIELD_COMMENT_PREFIX)
                append(it.toString())
                append(" */")
            }

            withIndent {
                printCollectionIfNotEmpty(property.accessors, prefix = "\n", separator = "\n") {
                    it.accept(explicitThis)
                }
            }
        }

        override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
            withSuffix(" ") { accessor.modifierList?.accept(this) }
            if (accessor.isGetter) {
                append("get")
            } else {
                append("set")
            }

            accessor.parameterList?.accept(this)
            withPrefix(": ") { accessor.typeReference?.accept(this) }
            if (accessor.hasBody()) {
                append(" { $DECOMPILED_CODE_COMMENT }")
            }
        }

        override fun visitParameterList(list: KtParameterList) {
            printCollection(list.parameters, prefix = "(", postfix = ")") {
                it.accept(explicitThis)
            }
        }

        override fun visitParameter(parameter: KtParameter) {
            withSuffix(" ") { parameter.modifierList?.accept(this) }
            append(parameter.name?.quoteIfNeeded())
            append(": ")
            parameter.typeReference?.accept(this)
            if (parameter.hasDefaultValue()) {
                append(" = $COMPILED_DEFAULT_PARAMETER_VALUE")
            }
        }

        override fun visitTypeParameterList(list: KtTypeParameterList) {
            printCollection(list.parameters, prefix = "<", postfix = ">") {
                it.accept(explicitThis)
            }
        }

        override fun visitTypeConstraintList(list: KtTypeConstraintList) {
            append("where ")
            printCollection(list.constraints, separator = ", ") {
                it.accept(explicitThis)
            }
        }

        override fun visitTypeConstraint(constraint: KtTypeConstraint) {
            // There is no need to print annotations as they are prohibited in type constraints

            constraint.subjectTypeParameterName?.accept(this)
            append(" : ")
            constraint.boundTypeReference?.accept(this)
        }

        override fun visitModifierList(list: KtModifierList) {
            " ".separated(
                { list.contextReceiverList?.accept(this) },
                { printAnnotations(list) },
                { printModifiers(list) },
            )
        }

        override fun visitContextReceiverList(contextReceiverList: KtContextReceiverList) {
            printCollection(contextReceiverList.contextReceivers(), prefix = "context(", postfix = ")") {
                it.accept(explicitThis)
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
            printCollectionIfNotEmpty(container.annotationEntries, separator = " ") {
                it.accept(explicitThis)
            }
        }

        override fun visitSuperTypeList(list: KtSuperTypeList) {
            printCollection(list.entries) {
                it.accept(explicitThis)
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
    printCollectionIfNotEmpty(declarations, separator = "\n\n", postfix = "\n") {
        it.accept(visitor)
    }
}.toString()

private enum class TypeReferencePosition {
    REGULAR,
    DECLARATION_RECEIVER,
    FUNCTION_TYPE_RECEIVER,
    CONTEXT_RECEIVER,
    ;
}
