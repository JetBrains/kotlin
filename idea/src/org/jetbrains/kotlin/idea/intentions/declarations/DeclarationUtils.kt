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

package org.jetbrains.kotlin.idea.intentions.declarations

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.setType
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

// returns assignment which replaces initializer
public fun splitPropertyDeclaration(property: JetProperty): JetBinaryExpression {
    val parent = property.getParent()!!

    val initializer = property.getInitializer()!!

    val explicitTypeToSet = if (property.getTypeReference() != null) null else initializer.analyze().getType(initializer)

    val psiFactory = JetPsiFactory(property)
    var assignment = psiFactory.createExpressionByPattern("$0 = $1", property.getName()!!, initializer)

    assignment = parent.addAfter(assignment, property) as JetBinaryExpression
    parent.addAfter(psiFactory.createNewLine(), property)

    property.setInitializer(null)

    if (explicitTypeToSet != null) {
        property.setType(explicitTypeToSet)
    }

    return assignment
}
