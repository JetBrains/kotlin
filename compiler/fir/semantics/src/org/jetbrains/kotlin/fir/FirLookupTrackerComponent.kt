/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCallInfo
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

abstract class FirLookupTrackerComponent : FirSessionComponent {

    abstract fun recordLookup(name: Name, inScopes: List<String>, source: KtSourceElement?, fileSource: KtSourceElement?)

    abstract fun recordLookup(name: Name, inScope: String, source: KtSourceElement?, fileSource: KtSourceElement?)
}

fun FirLookupTrackerComponent.recordCallLookup(callInfo: AbstractCallInfo, inType: ConeKotlinType) {
    val classId = inType.classId
    if (classId?.isLocal == true) return
    val scopes = SmartList(inType.renderForDebugging().replace('/', '.'))
    if (classId != null && classId.shortClassName.asString() == "Companion") {
        classId.outerClassId?.asString()?.replace('/', '.')?.let {
            scopes.add(it)
        }
    }
    recordLookup(callInfo.name, scopes, callInfo.callSite.source, callInfo.containingFile.source)
}

fun FirLookupTrackerComponent.recordCallLookup(callInfo: AbstractCallInfo, inScopes: List<String>) {
    recordLookup(callInfo.name, inScopes, callInfo.callSite.source, callInfo.containingFile.source)
}

fun FirLookupTrackerComponent.recordTypeLookup(typeRef: FirTypeRef, inScopes: List<String>, fileSource: KtSourceElement?) {
    if (typeRef is FirUserTypeRef) recordLookup(typeRef.qualifier.first().name, inScopes, typeRef.source, fileSource)
}

fun FirLookupTrackerComponent.recordTypeResolveAsLookup(typeRef: FirTypeRef, source: KtSourceElement?, fileSource: KtSourceElement?) {
    if (typeRef !is FirResolvedTypeRef) return // TODO: check if this is the correct behavior
    recordTypeResolveAsLookup(typeRef.type, source, fileSource)
}

fun FirLookupTrackerComponent.recordTypeResolveAsLookup(type: ConeKotlinType?, source: KtSourceElement?, fileSource: KtSourceElement?) {
    if (type == null) return
    if (source == null && fileSource == null) return // TODO: investigate all cases
    if (type is ConeErrorType) return // TODO: investigate whether some cases should be recorded, e.g. unresolved
    type.classId?.let {
        if (!it.isLocal) {
            if (it.shortClassName.asString() != "Companion") {
                recordLookup(it.shortClassName, it.packageFqName.asString(), source, fileSource)
            } else {
                recordLookup(it.outerClassId!!.shortClassName, it.outerClassId!!.packageFqName.asString(), source, fileSource)
            }
        }
    }
    type.typeArguments.forEach {
        if (it is ConeKotlinType) recordTypeResolveAsLookup(it, source, fileSource)
    }
}


val FirSession.lookupTracker: FirLookupTrackerComponent? by FirSession.nullableSessionComponentAccessor()
