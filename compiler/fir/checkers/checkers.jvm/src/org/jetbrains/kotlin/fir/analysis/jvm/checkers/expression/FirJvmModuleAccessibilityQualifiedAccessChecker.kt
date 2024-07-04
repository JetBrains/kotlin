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
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.java.JavaBinarySourceElement
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

object FirJvmModuleAccessibilityQualifiedAccessChecker : FirQualifiedAccessExpressionChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (callableSymbol.origin.fromSource) return

        val containingClass = callableSymbol.containingClassLookupTag()
        if (containingClass != null) {
            val containingClassSymbol = containingClass.toRegularClassSymbol(context.session) ?: return
            checkClassAccess(context, containingClassSymbol, expression, reporter)
        } else {
            val containerSource = callableSymbol.containerSource as? JvmPackagePartSource ?: return
            val virtualFile = (containerSource.knownJvmBinaryClass as? VirtualFileKotlinClass)?.file ?: return
            checkPackageAccess(context, virtualFile, containerSource.className.packageFqName, expression, reporter)
        }
    }

    internal fun checkClassAccess(context: CheckerContext, symbol: FirClassSymbol<*>, element: FirElement, reporter: DiagnosticReporter) {
        if (symbol.origin.fromSource) return

        @OptIn(SymbolInternals::class)
        val sourceElement = symbol.fir.sourceElement
        val virtualFile = when (sourceElement) {
            is JavaBinarySourceElement -> sourceElement.virtualFile
            is KotlinJvmBinarySourceElement -> (sourceElement.binaryClass as? VirtualFileKotlinClass)?.file ?: return
            else -> return
        }

        checkPackageAccess(context, virtualFile, symbol.packageFqName(), element, reporter)
    }

    internal fun checkPackageAccess(
        context: CheckerContext,
        fileFromPackage: VirtualFile,
        referencedPackageFqName: FqName,
        element: FirElement,
        reporter: DiagnosticReporter,
    ) {
        val fileFromOurModule = (context.containingFile?.sourceFile as? KtVirtualFileSourceFile)?.virtualFile

        val diagnostic = context.session.javaModuleResolverProvider.javaModuleResolver.checkAccessibility(
            fileFromOurModule, fileFromPackage, referencedPackageFqName
        ) ?: return

        val source = element.toReference(context.session)?.source ?: element.source
        when (diagnostic) {
            is JavaModuleResolver.AccessError.ModuleDoesNotExportPackage -> {
                reporter.reportOn(
                    source, FirJvmErrors.JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE, diagnostic.dependencyModuleName,
                    referencedPackageFqName.asString(), context,
                )
            }
            is JavaModuleResolver.AccessError.ModuleDoesNotReadModule -> {
                reporter.reportOn(source, FirJvmErrors.JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE, diagnostic.dependencyModuleName, context)
            }
            JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule -> {
                reporter.reportOn(source, FirJvmErrors.JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE, context)
            }
        }
    }
}
