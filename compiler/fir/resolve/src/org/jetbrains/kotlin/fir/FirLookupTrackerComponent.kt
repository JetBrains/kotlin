/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.resolve.calls.CallInfo
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

abstract class FirLookupTrackerComponent : FirSessionComponent {

    abstract fun recordLookup(name: Name, inScopes: List<String>, source: FirSourceElement?, fileSource: FirSourceElement?)

    abstract fun recordLookup(name: Name, inScope: String, source: FirSourceElement?, fileSource: FirSourceElement?)
}

fun FirLookupTrackerComponent.recordCallLookup(callInfo: CallInfo, inType: ConeKotlinType) {
    if (inType.classId?.isLocal == true) return
    val scopes = SmartList(inType.render().replace('/', '.'))
    if (inType.classId?.shortClassName?.asString() == "Companion") {
        scopes.add(inType.classId!!.outerClassId!!.asString().replace('/', '.'))
    }
    recordLookup(callInfo.name, scopes, callInfo.callSite.source, callInfo.containingFile.source)
}

fun FirLookupTrackerComponent.recordCallLookup(callInfo: CallInfo, inScopes: List<String>) {
    recordLookup(callInfo.name, inScopes, callInfo.callSite.source, callInfo.containingFile.source)
}

fun FirLookupTrackerComponent.recordTypeLookup(typeRef: FirTypeRef, inScopes: List<String>, fileSource: FirSourceElement?) {
    if (typeRef is FirUserTypeRef) recordLookup(typeRef.qualifier.first().name, inScopes, typeRef.source, fileSource)
}

fun FirLookupTrackerComponent.recordTypeResolveAsLookup(typeRef: FirTypeRef, source: FirSourceElement?, fileSource: FirSourceElement?) {
    if (typeRef !is FirResolvedTypeRef) return // TODO: check if this is the correct behavior
    if (source == null && fileSource == null) return // TODO: investigate all cases

    fun recordIfValid(type: ConeKotlinType) {
        if (type is ConeKotlinErrorType) return // TODO: investigate whether some cases should be recorded, e.g. unresolved
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
            if (it is ConeKotlinType) recordIfValid(it)
        }
    }

    recordIfValid(typeRef.type)
}


val FirSession.lookupTracker: FirLookupTrackerComponent? by FirSession.nullableSessionComponentAccessor()
