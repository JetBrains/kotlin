/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.isValhallaSupportEnabled
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaredMemberScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name

// Java serialization can't create an instance of a Valhalla value class, whose fields are strictly initialized, so one is serialized only
// through a `writeReplace` method, like javac warns.
object FirJvmSerializableValueClassChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    private val serializableClassId = ClassId.fromString("java/io/Serializable")
    private val writeReplaceName = Name.identifier("writeReplace")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!context.languageVersionSettings.isValhallaSupportEnabled()) return
        if (!declaration.isInlineOrValue || declaration.modality != Modality.FINAL) return
        if (declaration.hasAnnotation(JvmStandardClassIds.JVM_RECORD_ANNOTATION_CLASS_ID, context.session)) return
        val supertypes = lookupSuperTypes(declaration.symbol, lookupInterfaces = true, deep = true, context.session)
        if (supertypes.none { it.lookupTag.classId == serializableClassId }) return
        if (!declaration.symbol.hasWriteReplace()) {
            reporter.reportOn(declaration.source, FirJvmErrors.SERIALIZABLE_VALUE_CLASS_WITHOUT_WRITE_REPLACE)
        }
    }

    // Like Java serialization, this looks for a JVM method `writeReplace()` in the class and its superclasses, not in interfaces.
    context(context: CheckerContext)
    private fun FirRegularClassSymbol.hasWriteReplace(): Boolean {
        val superclasses = lookupSuperTypes(this, lookupInterfaces = false, deep = true, context.session)
        return (listOf(this) + superclasses.mapNotNull { it.toRegularClassSymbol(context.session) }).any { klass ->
            val scope = klass.declaredMemberScope()
            scope.getCallableNames().any { name ->
                scope.getFunctions(name).any {
                    it.jvmName() == writeReplaceName && it.valueParameterSymbols.isEmpty() && it.receiverParameterSymbol == null
                }
            }
        }
    }

    context(context: CheckerContext)
    private fun FirNamedFunctionSymbol.jvmName(): Name =
        getAnnotationByClassId(JvmStandardClassIds.Annotations.JvmName, context.session)
            ?.getStringArgument(StandardNames.NAME)?.let(Name::identifier) ?: name
}
