/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi.debugText

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

// invoke this instead of getText() when you need debug text to identify some place in PSI without storing the element itself
// this is need to avoid unnecessary file parses
// this defaults to get text if the element is not stubbed
fun KtElement.getDebugText(): String {
    if (this !is KtElementImplStub<*> || this.stub == null) {
        return text
    }
    if (this is KtPackageDirective) {
        val fqName = fqName
        if (fqName.isRoot) {
            return ""
        }
        return "package " + fqName.asString()
    }
    return accept(DebugTextBuildingVisitor, Unit)
}


private object DebugTextBuildingVisitor : KtVisitor<String, Unit>() {

    private val LOG = Logger.getInstance(this::class.java)

    override fun visitKtFile(file: KtFile, data: Unit?): String? {
        return "STUB file: ${file.name}"
    }

    override fun visitKtElement(element: KtElement, data: Unit?): String? {
        if (element is KtElementImplStub<*>) {
            LOG.error("getDebugText() is not defined for ${element::class.java}")
        }
        return element.text
    }

    override fun visitImportDirective(importDirective: KtImportDirective, data: Unit?): String? {
        val importPath = importDirective.importPath ?: return "import <invalid>"
        val aliasStr = if (importPath.hasAlias()) " as " + importPath.alias!!.asString() else ""
        return "import ${importPath.pathStr}" + aliasStr
    }

    override fun visitImportList(importList: KtImportList, data: Unit?): String? {
        return renderChildren(importList, separator = "\n")
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Unit?): String? {
        return render(annotationEntry, annotationEntry.calleeExpression, annotationEntry.typeArgumentList)
    }

    override fun visitTypeReference(typeReference: KtTypeReference, data: Unit?): String? {
        return renderChildren(typeReference, " ")
    }

    override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList, data: Unit?): String? {
        return renderChildren(typeArgumentList, ", ", "<", ">")
    }

    override fun visitTypeConstraintList(list: KtTypeConstraintList, data: Unit?): String? {
        return renderChildren(list, ", ", "where ", "")
    }

    override fun visitUserType(userType: KtUserType, data: Unit?): String? {
        return render(userType, userType.qualifier, userType.referenceExpression, userType.typeArgumentList)
    }

    override fun visitDynamicType(type: KtDynamicType, data: Unit?): String? {
        return "dynamic"
    }

    override fun visitAnnotation(annotation: KtAnnotation, data: Unit?): String? {
        return renderChildren(annotation, " ", "[", "]")
    }

    override fun visitConstructorCalleeExpression(constructorCalleeExpression: KtConstructorCalleeExpression, data: Unit?): String? {
        return render(constructorCalleeExpression, constructorCalleeExpression.constructorReferenceExpression)
    }

    override fun visitSuperTypeListEntry(specifier: KtSuperTypeListEntry, data: Unit?): String? {
        return render(specifier, specifier.typeReference)
    }

    override fun visitSuperTypeList(list: KtSuperTypeList, data: Unit?): String? {
        return renderChildren(list, ", ")
    }

    override fun visitTypeParameterList(list: KtTypeParameterList, data: Unit?): String? {
        return renderChildren(list, ", ", "<", ">")
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression, data: Unit?): String? {
        return renderChildren(expression, ".")
    }

    override fun visitInitializerList(list: KtInitializerList, data: Unit?): String? {
        return renderChildren(list, ", ")
    }

    override fun visitParameterList(list: KtParameterList, data: Unit?): String? {
        return renderChildren(list, ", ", "(", ")")
    }

    override fun visitEnumEntry(enumEntry: KtEnumEntry, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(enumEntry.modifierList, suffix = " ")
            append("enum entry ")
            appendInn(enumEntry.nameAsName)
            appendInn(enumEntry.initializerList, prefix = " : ")
        }
    }

    override fun visitFunctionType(functionType: KtFunctionType, data: Unit?): String? {
        return buildText {
            appendInn(functionType.receiverTypeReference, suffix = ".")
            appendInn(functionType.parameterList)
            appendInn(functionType.returnTypeReference, prefix = " -> ")
        }
    }

    override fun visitTypeParameter(parameter: KtTypeParameter, data: Unit?): String? {
        return buildText {
            appendInn(parameter.modifierList, suffix = " ")
            appendInn(parameter.nameAsName)
            appendInn(parameter.extendsBound, prefix = " : ")
        }
    }

    override fun visitTypeProjection(typeProjection: KtTypeProjection, data: Unit?): String? {
        return buildText {
            val token = typeProjection.projectionKind.token
            appendInn(token?.value)
            val typeReference = typeProjection.typeReference
            if (token != null && typeReference != null) {
                append(" ")
            }
            appendInn(typeReference)
        }
    }

    override fun visitModifierList(list: KtModifierList, data: Unit?): String? {
        return buildText {
            var first = true
            for (modifierKeywordToken in KtTokens.MODIFIER_KEYWORDS_ARRAY) {
                if (list.hasModifier(modifierKeywordToken)) {
                    if (!first) {
                        append(" ")
                    }
                    append(modifierKeywordToken.value)
                    first = false
                }
            }
        }
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Unit?): String? {
        return expression.getReferencedName()
    }

    override fun visitNullableType(nullableType: KtNullableType, data: Unit?): String? {
        return renderChildren(nullableType, "", "", "?")
    }

    override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: Unit?): String? {
        val containingDeclaration = KtStubbedPsiUtil.getContainingDeclaration(initializer)
        return "initializer in " + (containingDeclaration?.getDebugText() ?: "...")
    }

    override fun visitClassBody(classBody: KtClassBody, data: Unit?): String? {
        val containingDeclaration = KtStubbedPsiUtil.getContainingDeclaration(classBody)
        return "class body for " + (containingDeclaration?.getDebugText() ?: "...")
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor, data: Unit?): String? {
        val containingProperty = KtStubbedPsiUtil.getContainingDeclaration(accessor, KtProperty::class.java)
        val what = (if (accessor.isGetter) "getter" else "setter")
        return what + " for " + (containingProperty?.getDebugText() ?: "...")
    }

    override fun visitClass(klass: KtClass, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(klass.modifierList, suffix = " ")
            append("class ")
            appendInn(klass.nameAsName)
            appendInn(klass.typeParameterList)
            appendInn(klass.primaryConstructorModifierList, prefix = " ", suffix = " ")
            appendInn(klass.getPrimaryConstructorParameterList())
            appendInn(klass.getSuperTypeList(), prefix = " : ")
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(function.modifierList, suffix = " ")
            append("fun ")

            val typeParameterList = function.typeParameterList
            if (function.hasTypeParameterListBeforeFunctionName()) {
                appendInn(typeParameterList, suffix = " ")
            }
            appendInn(function.receiverTypeReference, suffix = ".")
            appendInn(function.nameAsName)
            if (!function.hasTypeParameterListBeforeFunctionName()) {
                appendInn(typeParameterList)
            }
            appendInn(function.valueParameterList)
            appendInn(function.typeReference, prefix = ": ")
            appendInn(function.typeConstraintList, prefix = " ")
        }
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(declaration.modifierList, suffix = " ")
            append("object ")
            appendInn(declaration.nameAsName)
            appendInn(declaration.getSuperTypeList(), prefix = " : ")
        }
    }

    override fun visitParameter(parameter: KtParameter, data: Unit?): String? {
        return buildText {
            if (parameter.hasValOrVar()) {
                if (parameter.isMutable) append("var ") else append("val ")
            }
            val name = parameter.nameAsName
            appendInn(name)
            val typeReference = parameter.typeReference
            if (typeReference != null && name != null) {
                append(": ")
            }
            appendInn(typeReference)
        }
    }

    override fun visitProperty(property: KtProperty, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(property.modifierList, suffix = " ")
            append(if (property.isVar) "var " else "val ")
            appendInn(property.nameAsName)
            appendInn(property.typeReference, prefix = ": ")
        }
    }

    override fun visitTypeConstraint(constraint: KtTypeConstraint, data: Unit?): String? {
        return buildText {
            appendInn(constraint.subjectTypeParameterName)
            appendInn(constraint.boundTypeReference, prefix = " : ")
        }
    }

    fun buildText(body: StringBuilder.() -> Unit): String? {
        val sb = StringBuilder()
        sb.body()
        return sb.toString()
    }

    fun renderChildren(element: KtElementImplStub<*>, separator: String, prefix: String = "", postfix: String = ""): String? {
        val childrenTexts = element.stub?.childrenStubs?.mapNotNull { (it?.psi as? KtElement)?.getDebugText() }
        return childrenTexts?.joinToString(separator, prefix, postfix) ?: element.text
    }

    fun render(element: KtElementImplStub<*>, vararg relevantChildren: KtElement?): String? {
        if (element.stub == null) return element.text
        return relevantChildren.filterNotNull().joinToString("", "", "") { it.getDebugText() }
    }
}

private fun StringBuilder.appendInn(target: Any?, prefix: String = "", suffix: String = "") {
    if (target == null) return
    append(prefix)
    append(when (target) {
               is KtElement -> target.getDebugText()
               else -> target.toString()
           })
    append(suffix)
}
