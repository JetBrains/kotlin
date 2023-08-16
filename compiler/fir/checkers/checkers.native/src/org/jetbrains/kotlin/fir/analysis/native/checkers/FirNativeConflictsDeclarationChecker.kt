package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.PlatformConflictDeclarationsDiagnosticDispatcher
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.backend.native.interop.getObjCMethodInfoFromOverriddenFunctions
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeStandardInteropNames

private val objCSignatureClassId = ClassId(NativeStandardInteropNames.cInteropPackage, Name.identifier("ExperimentalObjCSignature"))

private fun FirFunctionSymbol<*>.isInheritedFromObjc(context: CheckerContext): Boolean {
    return getObjCMethodInfoFromOverriddenFunctions(context.session, context.scopeSession) != null
}

private fun FirFunctionSymbol<*>.hasDifferentParameterNames(other: FirFunctionSymbol<*>) : Boolean {
    return valueParameterSymbols.drop(1).map { it.name } != other.valueParameterSymbols.drop(1).map { it.name }
}

fun NativeConflictDeclarationsDiagnosticDispatcher() = PlatformConflictDeclarationsDiagnosticDispatcher dispatcher@{ declaration, symbols, context ->
    if (declaration is FirFunction && symbols.all { it is FirFunctionSymbol<*> }) {
        if (declaration.symbol.isInheritedFromObjc(context) && symbols.all { (it as FirFunctionSymbol<*>).isInheritedFromObjc(context) }) {
            if (symbols.all { (it as FirFunctionSymbol<*>).hasDifferentParameterNames(declaration.symbol) }) {
                if (declaration.hasAnnotation(objCSignatureClassId, context.session)) {
                    return@dispatcher null
                } else {
                    return@dispatcher FirNativeErrors.CONFLICTING_OBJC_OVERLOADS
                }
            }
        }
    }
    PlatformConflictDeclarationsDiagnosticDispatcher.DEFAULT.getDiagnostic(declaration, symbols, context)
}