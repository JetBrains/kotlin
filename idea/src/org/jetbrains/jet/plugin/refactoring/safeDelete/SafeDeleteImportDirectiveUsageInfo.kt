/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.safeDelete

import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import org.jetbrains.kotlin.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.caches.resolve.analyze

public class SafeDeleteImportDirectiveUsageInfo(
        importDirective: JetImportDirective, declaration: JetDeclaration
) : SafeDeleteReferenceSimpleDeleteUsageInfo(importDirective, declaration, importDirective.isSafeToDelete(declaration))

fun JetImportDirective.isSafeToDelete(declaration: JetDeclaration): Boolean {
    val importExpr = getImportedReference()
    val importReference: JetReferenceExpression? = when (importExpr) {
        is JetSimpleNameExpression ->
            importExpr
        is JetDotQualifiedExpression ->
            importExpr.getSelectorExpression()?.let { selector ->
                if (selector is JetSimpleNameExpression) selector else null
            }
        else -> null
    }

    if (importReference != null) {
        val bindingContext = importReference.analyze()
        val referenceDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, importReference)
        val declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
        return referenceDescriptor == declarationDescriptor
    }
    return false
}
