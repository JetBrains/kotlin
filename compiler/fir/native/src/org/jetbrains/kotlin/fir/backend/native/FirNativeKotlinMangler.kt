/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.native

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirMangler
import org.jetbrains.kotlin.fir.backend.FirExportCheckerVisitor
import org.jetbrains.kotlin.fir.backend.FirMangleComputer
import org.jetbrains.kotlin.fir.backend.native.interop.*
import org.jetbrains.kotlin.fir.backend.native.interop.hasObjCFactoryAnnotation
import org.jetbrains.kotlin.fir.backend.native.interop.hasObjCMethodAnnotation
import org.jetbrains.kotlin.fir.backend.native.interop.isObjCClassMethod
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.name.NativeRuntimeNames

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

        if (annotations.hasAnnotation(NativeRuntimeNames.Annotations.symbolNameClassId, moduleData.session)) {
            // Treat any `@SymbolName` declaration as exported.
            return true
        }
        if (annotations.hasAnnotation(NativeRuntimeNames.Annotations.gcUnsafeCallClassId, moduleData.session)) {
            // Treat any `@GCUnsafeCall` declaration as exported.
            return true
        }
        if (annotations.hasAnnotation(NativeRuntimeNames.Annotations.exportForCppRuntimeClassId, moduleData.session)) {
            // Treat any `@ExportForCppRuntime` declaration as exported.
            return true
        }
        if (annotations.hasAnnotation(NativeRuntimeNames.Annotations.cNameClassId, moduleData.session)) {
            // Treat `@CName` declaration as exported.
            return true
        }
        if (annotations.hasAnnotation(NativeRuntimeNames.Annotations.exportForCompilerClassId, moduleData.session)) {
            return true
        }

        return false
    }
}

class FirNativeKotlinMangleComputer(
        builder: StringBuilder,
        mode: MangleMode,
) : FirMangleComputer(builder, mode) {
    override fun copy(newMode: MangleMode): FirNativeKotlinMangleComputer =
            FirNativeKotlinMangleComputer(builder, newMode)

    /**
     *  mimics FunctionDescriptor.platformSpecificFunctionName()
     */
    override fun FirFunction.platformSpecificFunctionName(): String? {
        val session = moduleData.session
        val scopeSession = ScopeSession()
        getInitMethodIfObjCConstructor(session, scopeSession)
                ?.getObjCMethodInfoFromOverriddenFunctions(session, scopeSession)
                ?.let {
                    return buildString {
                        receiverParameter?.let {
                            append(it.getTypeName(session))
                            append(".")
                        }

                        append("objc:")
                        append(it.selector)
                        if ((this@platformSpecificFunctionName is FirConstructor) && isObjCConstructor(session)) append("#Constructor")

                        if (this@platformSpecificFunctionName is FirPropertyAccessor) {
                            append("#Accessor")
                        }
                    }
                }
        return null
    }

    override fun FirFunction.specialValueParamPrefix(param: FirValueParameter): String {
        val session = moduleData.session
        return if (this.hasObjCMethodAnnotation(session) || this.hasObjCFactoryAnnotation(session) || this.isObjCClassMethod(session))
            "${param.name}:"
        else
            ""
    }
}

private fun FirReceiverParameter.getTypeName(session: FirSession): String {
    return when (val symbol = typeRef.coneType.toSymbol(session)) {
        is FirClassLikeSymbol -> symbol.classId.shortClassName.asString()
        is FirTypeParameterSymbol -> symbol.name.asString()
        else -> error("Unexpected symbol class: ${symbol?.javaClass?.name}")
    }
}
