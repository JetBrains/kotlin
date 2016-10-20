/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

fun move(container: PsiElement, statements: Array<PsiElement>, generateDefaultInitializers: Boolean): Array<PsiElement> {
    if (statements.isEmpty()) {
        return statements
    }

    val project = container.project

    val resultStatements = ArrayList<PsiElement>()
    val propertiesDeclarations = ArrayList<KtProperty>()

    // Dummy element to add new declarations at the beginning
    val psiFactory = KtPsiFactory(project)
    val dummyFirstStatement = container.addBefore(psiFactory.createExpression("dummyStatement"), statements[0])

    try {
        val scope = LocalSearchScope(container)
        val lastStatementOffset = statements[statements.size - 1].textRange.endOffset

        statements.forEachIndexed { i, statement ->
            if (needToDeclareOut(statement, lastStatementOffset, scope)) {
                if (statement is KtProperty && statement.initializer != null) {
                    if (i == statements.size - 1) {
                        kotlinStyleDeclareOut(container, dummyFirstStatement, resultStatements, propertiesDeclarations, statement)
                    }
                    else {
                        declareOut(container, dummyFirstStatement, generateDefaultInitializers, resultStatements, propertiesDeclarations, statement)
                    }
                }
                else {
                    val newStatement = container.addBefore(statement, dummyFirstStatement)
                    container.addAfter(psiFactory.createNewLine(), newStatement)
                    container.deleteChildRange(statement, statement)
                }
            }
            else {
                resultStatements.add(statement)
            }
        }
    }
    finally {
        dummyFirstStatement.delete()
    }

    ShortenReferences.DEFAULT.process(propertiesDeclarations)

    return PsiUtilCore.toPsiElementArray(resultStatements)
}

private fun kotlinStyleDeclareOut(container: PsiElement, dummyFirstStatement: PsiElement, resultStatements: ArrayList<PsiElement>,
                                  propertiesDeclarations: ArrayList<KtProperty>, statement: KtProperty) {
    var declaration = KtPsiFactory(statement).createProperty(statement.name!!, statement.typeReference?.text, statement.isVar, null)
    declaration = container.addBefore(declaration, dummyFirstStatement) as KtProperty
    container.addAfter(KtPsiFactory(declaration).createEQ(), declaration)
    propertiesDeclarations.add(declaration)
    val initializer = statement.initializer
    resultStatements.add(statement.replace(initializer!!))
}

private fun declareOut(container: PsiElement, dummyFirstStatement: PsiElement, generateDefaultInitializers: Boolean, resultStatements: ArrayList<PsiElement>,
                       propertiesDeclarations: ArrayList<KtProperty>, statement: KtProperty) {
    var declaration = createVariableDeclaration(statement, generateDefaultInitializers)
    declaration = container.addBefore(declaration, dummyFirstStatement) as KtProperty
    propertiesDeclarations.add(declaration)
    val assignment = createVariableAssignment(statement)
    resultStatements.add(statement.replace(assignment))
}

private fun createVariableAssignment(property: KtProperty): KtBinaryExpression {
    val propertyName = property.name ?: error("Property should have a name " + property.text)
    val assignment = KtPsiFactory(property).createExpression(propertyName + " = x") as KtBinaryExpression
    val right = assignment.right ?: error("Created binary expression should have a right part " + assignment.text)
    val initializer = property.initializer ?: error("Initializer should exist for property " + property.text)
    right.replace(initializer)
    return assignment
}

private fun createVariableDeclaration(property: KtProperty, generateDefaultInitializers: Boolean): KtProperty {
    val propertyType = getPropertyType(property)
    var defaultInitializer: String? = null
    if (generateDefaultInitializers && property.isVar) {
        defaultInitializer = CodeInsightUtils.defaultInitializer(propertyType)
    }
    return createProperty(property, propertyType, defaultInitializer)
}

private fun getPropertyType(property: KtProperty): KotlinType {
    val bindingContext = property.analyze(BodyResolveMode.PARTIAL)

    val propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property) ?: error("Couldn't resolve property to property descriptor " + property.text)
    return propertyDescriptor.type
}

private fun createProperty(property: KtProperty, propertyType: KotlinType, initializer: String?): KtProperty {
    val typeRef = property.typeReference
    val typeString = when {
        typeRef != null -> typeRef.text
        !propertyType.isError -> IdeDescriptorRenderers.SOURCE_CODE.renderType(propertyType)
        else -> null
    }
    return KtPsiFactory(property).createProperty(property.name!!, typeString, property.isVar, initializer)
}

private fun needToDeclareOut(element: PsiElement, lastStatementOffset: Int, scope: SearchScope): Boolean {
    if (element is KtProperty ||
        element is KtClassOrObject ||
        element is KtFunction) {

        val refs = ReferencesSearch.search(element, scope, false).toArray(PsiReference.EMPTY_ARRAY)
        if (refs.isNotEmpty()) {
            val lastRef = refs[refs.size - 1]
            if (lastRef.element.textOffset > lastStatementOffset) {
                return true
            }
        }
    }
    return false
}
