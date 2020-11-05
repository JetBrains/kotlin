/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerContext
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.assertIsValid
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext

internal class KtFirTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtTypeProvider(), KtFirAnalysisSessionComponent {
    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType = withValidityAssertion {
        val firDeclaration = declaration.getOrBuildFirOfType<FirCallableDeclaration<*>>(firResolveState)
        firDeclaration.returnTypeRef.coneType.asKtType()
    }

    override fun getKtExpressionType(expression: KtExpression): KtType = withValidityAssertion {
        expression.getOrBuildFirOfType<FirExpression>(firResolveState).typeRef.coneType.asKtType()
    }

    override fun isEqualTo(first: KtType, second: KtType): Boolean = withValidityAssertion {
        second.assertIsValid()
        check(first is KtFirType)
        check(second is KtFirType)
        return AbstractTypeChecker.equalTypes(
            this.createTypeCheckerContext() as AbstractTypeCheckerContext,
            first.coneType,
            second.coneType
        )
    }

    override fun isSubTypeOf(subType: KtType, superType: KtType): Boolean = withValidityAssertion {
        superType.assertIsValid()
        check(subType is KtFirType)
        check(superType is KtFirType)
        return AbstractTypeChecker.isSubtypeOf(
            this.createTypeCheckerContext() as AbstractTypeCheckerContext,
            subType.coneType,
            superType.coneType
        )
    }

    override fun isBuiltinFunctionalType(type: KtType): Boolean = withValidityAssertion {
        check(type is KtFirType)
        type.coneType.isBuiltinFunctionalType(analysisSession.firResolveState.rootModuleSession) //TODO use correct session here
    }

    private fun createTypeCheckerContext() = ConeTypeCheckerContext(
        isErrorTypeEqualsToAnything = true,
        isStubTypeEqualsToAnything = true,
        analysisSession.firResolveState.rootModuleSession //TODO use correct session here
    )
}