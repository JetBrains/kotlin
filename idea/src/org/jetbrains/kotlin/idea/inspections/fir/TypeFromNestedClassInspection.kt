/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.fir

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.idea.fir.coneTypeSafe
import org.jetbrains.kotlin.idea.fir.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.session
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class TypeFromNestedClassInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return namedFunctionVisitor(fun(function: KtNamedFunction) {
            if (function.isLocal) return
            val returnTypeReference = function.typeReference ?: return
            val firFunction = function.getOrBuildFir() as FirNamedFunction
            val firFunctionId = (firFunction.symbol as? FirFunctionSymbol)?.callableId ?: return

            val coneType = firFunction.coneTypeSafe as? ConeClassLikeType ?: return
            val typeId = (coneType.lookupTag.toSymbol(function.session) as? ConeClassSymbol)?.classId ?: return

            if (firFunctionId.packageName != typeId.packageFqName) return
            val functionClassName = firFunctionId.className ?: return
            var typeParentName = typeId.relativeClassName.parent()
            while (!typeParentName.isRoot) {
                if (typeParentName == functionClassName) {
                    holder.registerProblem(
                        returnTypeReference,
                        "I don't like when type from nested class is used as member return type",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                    return
                }
                typeParentName = typeParentName.parent()
            }
        })
    }
}