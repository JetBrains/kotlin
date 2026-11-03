/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.isValhallaSupportEnabled
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
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
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionBlockMember
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.isAnyOrNullableAny
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

// Java serialization can't create an instance of a Valhalla value class, whose fields are strictly initialized, so one is serialized only
// through a `writeReplace` method, like javac warns. It runs on the platform, where the expect superclasses have the members of their
// actual classes.
object FirJvmSerializableValueClassChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    private val serializableClassId = ClassId.fromString("java/io/Serializable")
    private val writeReplaceName = Name.identifier("writeReplace")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!context.languageVersionSettings.isValhallaSupportEnabled()) return
        if (!declaration.isValue || declaration.modality != Modality.FINAL) return
        if (declaration.hasAnnotation(JvmStandardClassIds.JVM_RECORD_ANNOTATION_CLASS_ID, context.session)) return
        val supertypes = lookupSuperTypes(declaration.symbol, lookupInterfaces = true, deep = true, context.session)
        if (supertypes.none { it.lookupTag.classId == serializableClassId }) return
        if (!declaration.symbol.hasWriteReplace()) {
            reporter.reportOn(declaration.source, FirJvmErrors.SERIALIZABLE_VALUE_CLASS_WITHOUT_WRITE_REPLACE)
        }
    }

    // Like `ObjectStreamClass.getInheritableMethod`, takes the first class of the superclass chain that declares a JVM method
    // `writeReplace()`, and checks that serialization can call it on this class.
    context(context: CheckerContext)
    private fun FirRegularClassSymbol.hasWriteReplace(): Boolean {
        var klass: FirRegularClassSymbol? = this
        val visited = mutableSetOf<FirRegularClassSymbol>()
        while (klass != null && klass.classId != StandardClassIds.Any && visited.add(klass)) {
            // Without the platform module, as in the metadata compilation, an expect superclass lacks the members of its actual class.
            if (klass.isExpect) return true
            val writeReplace = klass.declaredWriteReplace()
            if (writeReplace != null) return writeReplace.isCallableOn(this, declaredIn = klass)
            klass = klass.getSuperClassSymbolOrAny(context.session)
        }
        return false
    }

    context(context: CheckerContext)
    private fun FirRegularClassSymbol.declaredWriteReplace(): FirNamedFunctionSymbol? {
        val scope = declaredMemberScope()
        return scope.getCallableNames().asSequence().flatMap { scope.getFunctions(it) }.firstOrNull {
            it.jvmName() == writeReplaceName && it.valueParameterSymbols.isEmpty() && it.contextParameterSymbols.isEmpty() &&
                    it.receiverParameterSymbol == null
        }
    }

    // The method must be an instance method returning `Object`, and accessible from the serialized class.
    context(context: CheckerContext)
    private fun FirNamedFunctionSymbol.isCallableOn(serialized: FirRegularClassSymbol, declaredIn: FirRegularClassSymbol): Boolean {
        if (isCompanionBlockMember || modality == Modality.ABSTRACT) return false
        if (!resolvedReturnType.fullyExpandedType().isAnyOrNullableAny) return false
        return when (visibility) {
            Visibilities.Private -> declaredIn == serialized
            JavaVisibilities.PackageVisibility -> declaredIn.classId.packageFqName == serialized.classId.packageFqName
            else -> true
        }
    }

    // An internal function without `@JvmName` has a mangled JVM name.
    context(context: CheckerContext)
    private fun FirNamedFunctionSymbol.jvmName(): Name? {
        val explicitName =
            getAnnotationByClassId(JvmStandardClassIds.Annotations.JvmName, context.session)?.getStringArgument(StandardNames.NAME)
        return when {
            explicitName != null -> Name.identifier(explicitName)
            visibility == Visibilities.Internal -> null
            else -> name
        }
    }
}
