/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.annotations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.coneClassLikeType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolved
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.ResolveType
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.FirRefWithValidityCheck
import org.jetbrains.kotlin.name.ClassId


internal fun FirAnnotationCall.getClassId(session: FirSession): ClassId? =
    coneClassLikeType?.fullyExpandedType(session)?.classId

internal fun FirRefWithValidityCheck<FirAnnotatedDeclaration>.toAnnotationsList() = withFir { fir ->
    fir.annotations.map { KtFirAnnotationCall(this, it) }
}

internal fun FirRefWithValidityCheck<FirAnnotatedDeclaration>.containsAnnotation(classId: ClassId): Boolean =
    withFir(ResolveType.AnnotationType) { fir ->
        fir.annotations.any { it.getClassId(fir.moduleData.session) == classId }
    }

internal fun FirRefWithValidityCheck<FirAnnotatedDeclaration>.getAnnotationClassIds(): Collection<ClassId> =
    withFir(ResolveType.AnnotationType) { fir ->
        fir.annotations.mapNotNull { it.getClassId(fir.moduleData.session) }
    }