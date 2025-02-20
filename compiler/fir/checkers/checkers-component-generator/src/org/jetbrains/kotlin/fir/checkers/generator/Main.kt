/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.fir.builder.SYNTAX_DIAGNOSTIC_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JS_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JVM_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.NATIVE_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.WASM_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.WEB_COMMON_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.ErrorListDiagnosticListRenderer
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.generateDiagnostics
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.FirDynamicTypeRef
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirIntersectionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUnresolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import java.io.File

/*
    The first argument is the name of the project to generate
     [checkers.jvm|checkers.js|checkers.native|checkers.wasm|checkers.web.common|raw-fir.common|checkers]

     The second argument is optional, and it's generationPath, there is a hardcoded default for each

     Without arguments, it will produce all projects in their default locations
 */
fun main(args: Array<String>) {
    val basePackage = "org.jetbrains.kotlin.fir.analysis"

    val generationPath = args.getOrNull(1)?.let { File(it) }

    val packageName = "$basePackage.diagnostics"
    if (args.isEmpty() || args[0] == "checkers.jvm") {
        generateDiagnostics(
            generationPath ?: File("compiler/fir/checkers/checkers.jvm/gen"),
            "$packageName.jvm",
            JVM_DIAGNOSTICS_LIST,
            starImportsToAdd = setOf(
                ErrorListDiagnosticListRenderer.BASE_PACKAGE,
                ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE
            )
        )
    }
    if (args.isEmpty() || args[0] == "checkers.js") {
        generateDiagnostics(
            generationPath ?: File("compiler/fir/checkers/checkers.js/gen"),
            "$packageName.js",
            JS_DIAGNOSTICS_LIST,
            starImportsToAdd = setOf(
                ErrorListDiagnosticListRenderer.BASE_PACKAGE,
                ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE
            )
        )
    }
    if (args.isEmpty() || args[0] == "checkers.native") {
        generateDiagnostics(
            generationPath ?: File("compiler/fir/checkers/checkers.native/gen"),
            "$packageName.native",
            NATIVE_DIAGNOSTICS_LIST,
            starImportsToAdd = setOf(
                ErrorListDiagnosticListRenderer.BASE_PACKAGE,
                ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE
            )
        )
    }
    if (args.isEmpty() || args[0] == "checkers.wasm") {
        generateDiagnostics(
            generationPath ?: File("compiler/fir/checkers/checkers.wasm/gen"),
            "$packageName.wasm",
            WASM_DIAGNOSTICS_LIST,
            starImportsToAdd = setOf(
                ErrorListDiagnosticListRenderer.BASE_PACKAGE,
                ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE
            )
        )
    }
    if (args.isEmpty() || args[0] == "checkers.web.common") {
        generateDiagnostics(
            generationPath ?: File("compiler/fir/checkers/checkers.web.common/gen"),
            "$packageName.web.common",
            WEB_COMMON_DIAGNOSTICS_LIST,
            starImportsToAdd = setOf(
                ErrorListDiagnosticListRenderer.BASE_PACKAGE,
                ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE
            )
        )
    }
    if (args.isEmpty() || args[0] == "raw-fir.common") {
        generateDiagnostics(
            generationPath ?: File("compiler/fir/raw-fir/raw-fir.common/gen"),
            "org.jetbrains.kotlin.fir.builder",
            SYNTAX_DIAGNOSTIC_LIST,
            starImportsToAdd = setOf(ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE)
        )
    }
    if (args.isEmpty() || args[0] == "checkers") {
        val checkersPath = generationPath ?: File("compiler/fir/checkers/checkers/gen")
        val typePackage = "$basePackage.checkers.type"
        generateCheckersComponents(checkersPath, typePackage, "FirTypeChecker", FirTypeRef::class, FirTypeRef::class) {
            alias<FirTypeRef>("TypeRefChecker").let {
                visitAlso<FirImplicitTypeRef>(it)
                visitAlso<FirUnresolvedTypeRef>(it)
                visitAlso<FirUserTypeRef>(it)
                visitAlso<FirDynamicTypeRef>(it)
            }
            alias<FirResolvedTypeRef>("ResolvedTypeRefChecker").let {
                visitAlso<FirErrorTypeRef>(it)
            }
            alias<FirFunctionTypeRef>("FunctionTypeRefChecker")
            alias<FirIntersectionTypeRef>("IntersectionTypeRefChecker")
        }

        val expressionPackage = "$basePackage.checkers.expression"
        generateCheckersComponents(checkersPath, expressionPackage, "FirExpressionChecker", FirStatement::class, FirExpression::class) {
            alias<FirStatement>("BasicExpressionChecker", false).let {
                visitAlso<FirExpression>(it)
                visitAlso<FirVarargArgumentsExpression>(it)
                visitAlso<FirSamConversionExpression>(it)
                visitAlso<FirWrappedExpression>(it)
                visitAlso<FirWrappedArgumentExpression>(it)
                visitAlso<FirSpreadArgumentExpression>(it)
                visitAlso<FirNamedArgumentExpression>(it)
                visitAlso<FirWhenSubjectExpression>(it)
                visitAlso<FirResolvedReifiedParameterReference>(it)
                visitAlso<FirComparisonExpression>(it)
                visitAlso<FirDesugaredAssignmentValueReferenceExpression>(it)
                visitAlso<FirCheckedSafeCallSubject>(it)
                visitAlso<FirErrorExpression>(it)
                visitAlso<FirQualifiedErrorAccessExpression>(it)
            }
            alias<FirQualifiedAccessExpression>("QualifiedAccessExpressionChecker")
            alias<FirCall>("CallChecker", false).let {
                visitAlso<FirDelegatedConstructorCall>(it)
                visitAlso<FirMultiDelegatedConstructorCall>(it)
            }
            alias<FirFunctionCall>("FunctionCallChecker").let {
                visitAlso<FirComponentCall>(it)
                visitAlso<FirImplicitInvokeCall>(it)
            }
            alias<FirPropertyAccessExpression>("PropertyAccessExpressionChecker")
            alias<FirIntegerLiteralOperatorCall>("IntegerLiteralOperatorCallChecker")
            alias<FirVariableAssignment>("VariableAssignmentChecker")
            alias<FirTryExpression>("TryExpressionChecker")
            alias<FirWhenExpression>("WhenExpressionChecker")
            alias<FirLoop>("LoopExpressionChecker", false).let {
                visitAlso<FirErrorLoop>(it)
            }
            alias<FirLoopJump>("LoopJumpChecker", false).let {
                visitAlso<FirBreakExpression>(it)
                visitAlso<FirContinueExpression>(it)
            }
            alias<FirBooleanOperatorExpression>("BooleanOperatorExpressionChecker")
            alias<FirReturnExpression>("ReturnExpressionChecker")
            alias<FirBlock>("BlockChecker")
            alias<FirAnnotation>("AnnotationChecker")
            alias<FirAnnotationCall>("AnnotationCallChecker").let {
                visitAlso<FirErrorAnnotationCall>(it)
            }
            alias<FirCheckNotNullCall>("CheckNotNullCallChecker")
            alias<FirElvisExpression>("ElvisExpressionChecker")
            alias<FirGetClassCall>("GetClassCallChecker")
            alias<FirSafeCallExpression>("SafeCallExpressionChecker")
            alias<FirSmartCastExpression>("SmartCastExpressionChecker")
            alias<FirEqualityOperatorCall>("EqualityOperatorCallChecker")
            alias<FirStringConcatenationCall>("StringConcatenationCallChecker")
            alias<FirTypeOperatorCall>("TypeOperatorCallChecker")
            alias<FirResolvedQualifier>("ResolvedQualifierChecker").let {
                visitAlso<FirErrorResolvedQualifier>(it)
            }
            alias<FirLiteralExpression>("LiteralExpressionChecker")
            alias<FirCallableReferenceAccess>("CallableReferenceAccessChecker")
            alias<FirThisReceiverExpression>("ThisReceiverExpressionChecker")
            alias<FirWhileLoop>("WhileLoopChecker")
            alias<FirThrowExpression>("ThrowExpressionChecker")
            alias<FirDoWhileLoop>("DoWhileLoopChecker")
            alias<FirArrayLiteral>("ArrayLiteralChecker")
            alias<FirClassReferenceExpression>("ClassReferenceExpressionChecker")
            alias<FirInaccessibleReceiverExpression>("InaccessibleReceiverChecker")
        }

        val declarationPackage = "$basePackage.checkers.declaration"
        generateCheckersComponents(
            checkersPath,
            declarationPackage,
            "FirDeclarationChecker",
            FirDeclaration::class,
            FirDeclaration::class
        ) {
            alias<FirDeclaration>("BasicDeclarationChecker").let {
                visitAlso<FirDanglingModifierList>(it)
                visitAlso<FirCodeFragment>(it)
            }
            alias<FirCallableDeclaration>("CallableDeclarationChecker", false).let {
                visitAlso<FirField>(it)
                visitAlso<FirErrorProperty>(it)
            }
            alias<FirFunction>("FunctionChecker", false)
            alias<FirSimpleFunction>("SimpleFunctionChecker")
            alias<FirProperty>("PropertyChecker")
            alias<FirClassLikeDeclaration>("ClassLikeChecker", false)
            alias<FirClass>("ClassChecker")
            alias<FirRegularClass>("RegularClassChecker")
            alias<FirConstructor>("ConstructorChecker").let {
                visitAlso<FirErrorPrimaryConstructor>(it)
            }
            alias<FirFile>("FileChecker")
            alias<FirScript>("ScriptChecker")
            alias<FirReplSnippet>("ReplSnippetChecker")
            alias<FirTypeParameter>("FirTypeParameterChecker")
            alias<FirTypeAlias>("TypeAliasChecker")
            alias<FirAnonymousFunction>("AnonymousFunctionChecker")
            alias<FirPropertyAccessor>("PropertyAccessorChecker")
            alias<FirBackingField>("BackingFieldChecker")
            alias<FirValueParameter>("ValueParameterChecker")
            alias<FirEnumEntry>("EnumEntryChecker")
            alias<FirAnonymousObject>("AnonymousObjectChecker")
            alias<FirAnonymousInitializer>("AnonymousInitializerChecker")
            alias<FirReceiverParameter>("ReceiverParameterChecker")

            additional(
                fieldName = "controlFlowAnalyserCheckers",
                classFqn = "$basePackage.checkers.cfa.FirControlFlowChecker"
            )

            additional(
                fieldName = "variableAssignmentCfaBasedCheckers",
                classFqn = "$basePackage.cfa.AbstractFirPropertyInitializationChecker"
            )
        }

        generateDiagnostics(
            checkersPath,
            packageName,
            DIAGNOSTICS_LIST,
            starImportsToAdd = setOf(
                ErrorListDiagnosticListRenderer.BASE_PACKAGE,
                ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE
            )
        )
        generateNonSuppressibleErrorNamesFile(checkersPath, packageName)
    }
}

/*
 * Stages:
 *   1. associate aliases with fir statements
 *   2. build inheritance hierarchy for all mentioned fir elements
 *   3. generate aliases
 *   4. generate abstract "DeclarationCheckers"
 *   5. generate "ComposedDeclarationCheckers" with OptIn annotation
 *   6. generate "DeclarationCheckersDiagnosticCompoment"
 */
