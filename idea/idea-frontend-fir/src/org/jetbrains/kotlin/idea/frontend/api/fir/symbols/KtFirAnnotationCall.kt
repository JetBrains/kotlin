/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.convertConstantExpression
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.mapAnnotationParameters
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

class KtFirAnnotationCall(
    containingDeclaration: FirDeclaration,
    annotationCall: FirAnnotationCall,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
) : KtAnnotationCall() {

    private val containingDeclarationRef = firRef(containingDeclaration, resolveState)
    private val annotationCallRef by weakRef(annotationCall)

    override val psi: KtCallElement? by containingDeclarationRef.withFirAndCache { fir ->
        annotationCallRef.findPsi(fir.session) as? KtCallElement
    }

    private fun ConeClassLikeType.expandTypeAliasIfNeeded(session: FirSession): ConeClassLikeType {
        val firTypeAlias = lookupTag.toSymbol(session) as? FirTypeAliasSymbol ?: return this
        val expandedType = firTypeAlias.fir.expandedTypeRef.coneType
        return expandedType.fullyExpandedType(session) as? ConeClassLikeType
            ?: return this
    }

    override val classId: ClassId? by containingDeclarationRef.withFirAndCache(FirResolvePhase.TYPES) {
        val declaredCone = annotationCallRef.annotationTypeRef.coneType as? ConeClassLikeType
        declaredCone?.expandTypeAliasIfNeeded(it.session)?.classId
    }

    override val useSiteTarget: AnnotationUseSiteTarget? get() = annotationCallRef.useSiteTarget

    override val arguments: List<KtNamedConstantValue> by containingDeclarationRef.withFirAndCache(FirResolvePhase.TYPES) {
        mapAnnotationParameters(annotationCallRef, it.session).map { (name, expression) ->
            KtNamedConstantValue(name, expression.convertConstantExpression())
        }
    }
}
