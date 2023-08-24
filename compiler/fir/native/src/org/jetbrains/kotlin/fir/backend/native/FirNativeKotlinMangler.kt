/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.native

import org.jetbrains.kotlin.backend.common.serialization.mangle.*
import org.jetbrains.kotlin.backend.konan.serialization.ANNOTATIONS_TO_TREAT_AS_EXPORTED
import org.jetbrains.kotlin.backend.konan.serialization.ObjCFunctionNameMangleComputer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirExportCheckerVisitor
import org.jetbrains.kotlin.fir.backend.FirMangleComputer
import org.jetbrains.kotlin.fir.backend.FirMangler
import org.jetbrains.kotlin.fir.backend.native.interop.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.interop.ObjCMethodInfo

class FirNativeKotlinMangler : FirMangler() {
    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<FirDeclaration> {
        return FirNativeKotlinMangleComputer(StringBuilder(256), mode)
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<FirDeclaration> = FirNativeExportCheckerVisitor()

    override fun FirDeclaration.isExported(compatibleMode: Boolean): Boolean =
            getExportChecker(compatibleMode).check(this, SpecialDeclarationType.REGULAR)
}

class FirNativeExportCheckerVisitor : FirExportCheckerVisitor() {
    /**
     * mimics AbstractKonanDescriptorMangler::DeclarationDescriptor.isPlatformSpecificExport()
     */
    override fun FirDeclaration.isPlatformSpecificExported(): Boolean {
        if (this is FirCallableDeclaration && isSubstitutionOrIntersectionOverride)
            return false
        return ANNOTATIONS_TO_TREAT_AS_EXPORTED.any { hasAnnotation(it, moduleData.session) }
    }
}

private class FirObjCFunctionNameMangleComputer(
    private val function: FirFunction
) : ObjCFunctionNameMangleComputer<FirValueParameter>() {

    private val session = function.moduleData.session

    override fun getObjCMethodInfo(): ObjCMethodInfo? {
        val scopeSession = ScopeSession()
        return function.getInitMethodIfObjCConstructor(session, scopeSession)
            ?.getObjCMethodInfoFromOverriddenFunctions(session, scopeSession)
    }

    override fun getExtensionReceiverClassName(): Name? = function.receiverParameter?.getTypeName(session)?.let(Name::identifier)

    override fun isObjCConstructor(): Boolean = function is FirConstructor && function.isObjCConstructor(session)

    override fun isPropertyAccessor(): Boolean = function is FirPropertyAccessor

    override fun hasObjCMethodAnnotation(): Boolean = function.hasObjCMethodAnnotation(session)

    override fun hasObjCFactoryAnnotation(): Boolean = function.hasObjCFactoryAnnotation(session)

    override fun isObjCClassMethod(): Boolean = function.isObjCClassMethod(session)

    override fun getValueParameterName(valueParameter: FirValueParameter): Name = valueParameter.name
}

class FirNativeKotlinMangleComputer(
        builder: StringBuilder,
        mode: MangleMode,
) : FirMangleComputer(builder, mode) {
    override fun copy(newMode: MangleMode): FirNativeKotlinMangleComputer =
            FirNativeKotlinMangleComputer(builder, newMode)

    override fun makePlatformSpecificFunctionNameMangleComputer(
        function: FirFunction
    ): PlatformSpecificFunctionNameMangleComputer<FirValueParameter> = FirObjCFunctionNameMangleComputer(function)
}

private fun FirReceiverParameter.getTypeName(session: FirSession): String {
    return when (val symbol = typeRef.coneType.toSymbol(session)) {
        is FirClassLikeSymbol -> symbol.classId.shortClassName.asString()
        is FirTypeParameterSymbol -> symbol.name.asString()
        else -> error("Unexpected symbol class: ${symbol?.javaClass?.name}")
    }
}
