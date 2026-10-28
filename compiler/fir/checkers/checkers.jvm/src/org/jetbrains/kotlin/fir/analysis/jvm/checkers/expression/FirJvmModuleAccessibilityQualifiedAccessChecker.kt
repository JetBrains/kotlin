/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.java.BinaryClassSourceElement
import org.jetbrains.kotlin.fir.java.VirtualFileBasedSourceElement
import org.jetbrains.kotlin.fir.modules.javaModuleResolverProvider
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver.AccessError

object FirJvmModuleAccessibilityQualifiedAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (callableSymbol.origin.fromSource) return

        val containingClass = callableSymbol.containingClassLookupTag()
        if (containingClass != null) {
            val containingClassSymbol = containingClass.toRegularClassSymbol() ?: return
            checkClassAccess(containingClassSymbol, expression)
        } else {
            val containerSource = callableSymbol.containerSource as? JvmPackagePartSource ?: return
            val virtualFile = (containerSource.knownJvmBinaryClass as? VirtualFileKotlinClass)?.file ?: return
            checkPackageAccess(virtualFile, containerSource.className.packageFqName, expression)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun checkClassAccess(
        symbol: FirClassSymbol<*>,
        element: FirElement
    ) {
        if (symbol.origin.fromSource) return

        @OptIn(SymbolInternals::class)
        val sourceElement = symbol.fir.sourceElement
        val packageFqName = symbol.packageFqName()
        when (sourceElement) {
            is BinaryClassSourceElement -> checkPackageAccess(packageFqName, element) { ourFile, resolver ->
                resolver.checkAccessibility(ourFile, sourceElement.classFile, packageFqName)
            }
            is VirtualFileBasedSourceElement -> checkPackageAccess(sourceElement.virtualFile, packageFqName, element)
            is KotlinJvmBinarySourceElement -> {
                val virtualFile = (sourceElement.binaryClass as? VirtualFileKotlinClass)?.file ?: return
                checkPackageAccess(virtualFile, packageFqName, element)
            }
            else -> return
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun checkPackageAccess(
        fileFromPackage: VirtualFile,
        referencedPackageFqName: FqName,
        element: FirElement,
    ) {
        checkPackageAccess(referencedPackageFqName, element) { ourFile, resolver ->
            resolver.checkAccessibility(ourFile, fileFromPackage, referencedPackageFqName)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private inline fun checkPackageAccess(
        referencedPackageFqName: FqName,
        element: FirElement,
        check: (fileFromOurModule: VirtualFile?, JavaModuleResolver) -> AccessError?,
    ) {
        val fileFromOurModule = (context.containingFileSymbol?.sourceFile as? KtVirtualFileSourceFile)?.virtualFile

        val diagnostic = check(fileFromOurModule, context.session.javaModuleResolverProvider.javaModuleResolver) ?: return

        val source = element.toReference(context.session)?.source ?: element.source
        when (diagnostic) {
            is AccessError.ModuleDoesNotExportPackage -> {
                reporter.reportOn(
                    source,
                    FirJvmErrors.JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE,
                    diagnostic.dependencyModuleName,
                    referencedPackageFqName.asString()
                )
            }
            is AccessError.ModuleDoesNotReadModule -> {
                reporter.reportOn(source, FirJvmErrors.JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE, diagnostic.dependencyModuleName)
            }
            AccessError.ModuleDoesNotReadUnnamedModule -> {
                reporter.reportOn(source, FirJvmErrors.JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE)
            }
        }
    }
}
