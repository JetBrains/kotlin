/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")
package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.visitors.FirElementKind.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatementStub
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.FirErrorImport
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirBinaryLogicExpression
import org.jetbrains.kotlin.fir.expressions.FirJump
import org.jetbrains.kotlin.fir.expressions.FirLoopJump
import org.jetbrains.kotlin.fir.expressions.FirBreakExpression
import org.jetbrains.kotlin.fir.expressions.FirContinueExpression
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirStarProjection
import org.jetbrains.kotlin.fir.types.FirPlaceholderProjection
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirAssignmentOperatorStatement
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirContextReceiverArgumentListOwner
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirAugmentedArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirIntegerLiteralOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirLambdaArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirResolvedReifiedParameterReference
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.FirBackingFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRefWithNullability
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.FirDynamicTypeRef
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirIntersectionTypeRef
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirSmartCastedTypeRef
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirVisitor<out R, in D> {
    abstract fun visitElement(element: FirElement, data: D): R


    fun dispatch(element: FirElement, data: D): R {
        return when (element.elementKind) {
            // Reference -> [FirStubReference]
            Reference -> visitReference(element as FirReference, data)
            Label -> visitLabel(element as FirLabel, data)
            DeclarationStatus -> visitDeclarationStatus(element as FirDeclarationStatus, data)
            ResolvedDeclarationStatus -> visitResolvedDeclarationStatus(element as FirResolvedDeclarationStatus, data)
            // Expression -> [FirElseIfTrueCondition, FirExpressionStub, FirLazyExpression, FirUnitExpression]
            Expression -> visitExpression(element as FirExpression, data)
            StatementStub -> visitStatementStub(element as FirStatementStub, data)
            ContextReceiver -> visitContextReceiver(element as FirContextReceiver, data)
            AnonymousInitializer -> visitAnonymousInitializer(element as FirAnonymousInitializer, data)
            // TypeParameterRef -> [FirOuterClassTypeParameterRef, FirConstructedClassTypeParameterRef]
            TypeParameterRef -> visitTypeParameterRef(element as FirTypeParameterRef, data)
            TypeParameter -> visitTypeParameter(element as FirTypeParameter, data)
            ValueParameter -> visitValueParameter(element as FirValueParameter, data)
            Property -> visitProperty(element as FirProperty, data)
            Field -> visitField(element as FirField, data)
            EnumEntry -> visitEnumEntry(element as FirEnumEntry, data)
            RegularClass -> visitRegularClass(element as FirRegularClass, data)
            TypeAlias -> visitTypeAlias(element as FirTypeAlias, data)
            SimpleFunction -> visitSimpleFunction(element as FirSimpleFunction, data)
            PropertyAccessor -> visitPropertyAccessor(element as FirPropertyAccessor, data)
            BackingField -> visitBackingField(element as FirBackingField, data)
            Constructor -> visitConstructor(element as FirConstructor, data)
            File -> visitFile(element as FirFile, data)
            PackageDirective -> visitPackageDirective(element as FirPackageDirective, data)
            AnonymousFunction -> visitAnonymousFunction(element as FirAnonymousFunction, data)
            AnonymousFunctionExpression -> visitAnonymousFunctionExpression(element as FirAnonymousFunctionExpression, data)
            AnonymousObject -> visitAnonymousObject(element as FirAnonymousObject, data)
            AnonymousObjectExpression -> visitAnonymousObjectExpression(element as FirAnonymousObjectExpression, data)
            // Import -> [null]
            Import -> visitImport(element as FirImport, data)
            ResolvedImport -> visitResolvedImport(element as FirResolvedImport, data)
            ErrorImport -> visitErrorImport(element as FirErrorImport, data)
            ErrorLoop -> visitErrorLoop(element as FirErrorLoop, data)
            DoWhileLoop -> visitDoWhileLoop(element as FirDoWhileLoop, data)
            WhileLoop -> visitWhileLoop(element as FirWhileLoop, data)
            Block -> visitBlock(element as FirBlock, data)
            BinaryLogicExpression -> visitBinaryLogicExpression(element as FirBinaryLogicExpression, data)
            BreakExpression -> visitBreakExpression(element as FirBreakExpression, data)
            ContinueExpression -> visitContinueExpression(element as FirContinueExpression, data)
            Catch -> visitCatch(element as FirCatch, data)
            TryExpression -> visitTryExpression(element as FirTryExpression, data)
            ConstExpression -> visitConstExpression(element as FirConstExpression<*>, data)
            StarProjection -> visitStarProjection(element as FirStarProjection, data)
            PlaceholderProjection -> visitPlaceholderProjection(element as FirPlaceholderProjection, data)
            TypeProjectionWithVariance -> visitTypeProjectionWithVariance(element as FirTypeProjectionWithVariance, data)
            ArgumentList -> visitArgumentList(element as FirArgumentList, data)
            // Annotation -> [null]
            Annotation -> visitAnnotation(element as FirAnnotation, data)
            AnnotationCall -> visitAnnotationCall(element as FirAnnotationCall, data)
            AnnotationArgumentMapping -> visitAnnotationArgumentMapping(element as FirAnnotationArgumentMapping, data)
            ComparisonExpression -> visitComparisonExpression(element as FirComparisonExpression, data)
            TypeOperatorCall -> visitTypeOperatorCall(element as FirTypeOperatorCall, data)
            AssignmentOperatorStatement -> visitAssignmentOperatorStatement(element as FirAssignmentOperatorStatement, data)
            EqualityOperatorCall -> visitEqualityOperatorCall(element as FirEqualityOperatorCall, data)
            WhenExpression -> visitWhenExpression(element as FirWhenExpression, data)
            WhenBranch -> visitWhenBranch(element as FirWhenBranch, data)
            CheckNotNullCall -> visitCheckNotNullCall(element as FirCheckNotNullCall, data)
            ElvisExpression -> visitElvisExpression(element as FirElvisExpression, data)
            ArrayOfCall -> visitArrayOfCall(element as FirArrayOfCall, data)
            AugmentedArraySetCall -> visitAugmentedArraySetCall(element as FirAugmentedArraySetCall, data)
            ClassReferenceExpression -> visitClassReferenceExpression(element as FirClassReferenceExpression, data)
            ErrorExpression -> visitErrorExpression(element as FirErrorExpression, data)
            ErrorFunction -> visitErrorFunction(element as FirErrorFunction, data)
            ErrorProperty -> visitErrorProperty(element as FirErrorProperty, data)
            PropertyAccessExpression -> visitPropertyAccessExpression(element as FirPropertyAccessExpression, data)
            // FunctionCall -> [null]
            FunctionCall -> visitFunctionCall(element as FirFunctionCall, data)
            IntegerLiteralOperatorCall -> visitIntegerLiteralOperatorCall(element as FirIntegerLiteralOperatorCall, data)
            ImplicitInvokeCall -> visitImplicitInvokeCall(element as FirImplicitInvokeCall, data)
            DelegatedConstructorCall -> visitDelegatedConstructorCall(element as FirDelegatedConstructorCall, data)
            ComponentCall -> visitComponentCall(element as FirComponentCall, data)
            CallableReferenceAccess -> visitCallableReferenceAccess(element as FirCallableReferenceAccess, data)
            ThisReceiverExpression -> visitThisReceiverExpression(element as FirThisReceiverExpression, data)
            SafeCallExpression -> visitSafeCallExpression(element as FirSafeCallExpression, data)
            CheckedSafeCallSubject -> visitCheckedSafeCallSubject(element as FirCheckedSafeCallSubject, data)
            GetClassCall -> visitGetClassCall(element as FirGetClassCall, data)
            LambdaArgumentExpression -> visitLambdaArgumentExpression(element as FirLambdaArgumentExpression, data)
            SpreadArgumentExpression -> visitSpreadArgumentExpression(element as FirSpreadArgumentExpression, data)
            NamedArgumentExpression -> visitNamedArgumentExpression(element as FirNamedArgumentExpression, data)
            VarargArgumentsExpression -> visitVarargArgumentsExpression(element as FirVarargArgumentsExpression, data)
            // ResolvedQualifier -> [null]
            ResolvedQualifier -> visitResolvedQualifier(element as FirResolvedQualifier, data)
            ErrorResolvedQualifier -> visitErrorResolvedQualifier(element as FirErrorResolvedQualifier, data)
            ResolvedReifiedParameterReference -> visitResolvedReifiedParameterReference(element as FirResolvedReifiedParameterReference, data)
            ReturnExpression -> visitReturnExpression(element as FirReturnExpression, data)
            StringConcatenationCall -> visitStringConcatenationCall(element as FirStringConcatenationCall, data)
            ThrowExpression -> visitThrowExpression(element as FirThrowExpression, data)
            VariableAssignment -> visitVariableAssignment(element as FirVariableAssignment, data)
            WhenSubjectExpression -> visitWhenSubjectExpression(element as FirWhenSubjectExpression, data)
            WrappedDelegateExpression -> visitWrappedDelegateExpression(element as FirWrappedDelegateExpression, data)
            // NamedReference -> [FirSimpleNamedReference]
            NamedReference -> visitNamedReference(element as FirNamedReference, data)
            ErrorNamedReference -> visitErrorNamedReference(element as FirErrorNamedReference, data)
            SuperReference -> visitSuperReference(element as FirSuperReference, data)
            ThisReference -> visitThisReference(element as FirThisReference, data)
            ControlFlowGraphReference -> visitControlFlowGraphReference(element as FirControlFlowGraphReference, data)
            // ResolvedNamedReference -> [FirPropertyFromParameterResolvedNamedReference, null]
            ResolvedNamedReference -> visitResolvedNamedReference(element as FirResolvedNamedReference, data)
            DelegateFieldReference -> visitDelegateFieldReference(element as FirDelegateFieldReference, data)
            BackingFieldReference -> visitBackingFieldReference(element as FirBackingFieldReference, data)
            ResolvedCallableReference -> visitResolvedCallableReference(element as FirResolvedCallableReference, data)
            // ResolvedTypeRef -> [null]
            ResolvedTypeRef -> visitResolvedTypeRef(element as FirResolvedTypeRef, data)
            ErrorTypeRef -> visitErrorTypeRef(element as FirErrorTypeRef, data)
            UserTypeRef -> visitUserTypeRef(element as FirUserTypeRef, data)
            DynamicTypeRef -> visitDynamicTypeRef(element as FirDynamicTypeRef, data)
            FunctionTypeRef -> visitFunctionTypeRef(element as FirFunctionTypeRef, data)
            IntersectionTypeRef -> visitIntersectionTypeRef(element as FirIntersectionTypeRef, data)
            ImplicitTypeRef -> visitImplicitTypeRef(element as FirImplicitTypeRef, data)
            SmartCastedTypeRef -> visitSmartCastedTypeRef(element as FirSmartCastedTypeRef, data)
            EffectDeclaration -> visitEffectDeclaration(element as FirEffectDeclaration, data)
            ContractDescription -> visitContractDescription(element as FirContractDescription, data)
            LegacyRawContractDescription -> visitLegacyRawContractDescription(element as FirLegacyRawContractDescription, data)
            RawContractDescription -> visitRawContractDescription(element as FirRawContractDescription, data)
            ResolvedContractDescription -> visitResolvedContractDescription(element as FirResolvedContractDescription, data)
        }
    }

    fun dispatchChildren(element: FirElement, data: D) {
        when (element.elementKind) {
            // Reference -> [FirStubReference]
            Reference -> visitReferenceChildren(element as FirReference, data)
            Label -> visitLabelChildren(element as FirLabel, data)
            DeclarationStatus -> visitDeclarationStatusChildren(element as FirDeclarationStatus, data)
            ResolvedDeclarationStatus -> visitResolvedDeclarationStatusChildren(element as FirResolvedDeclarationStatus, data)
            // Expression -> [FirElseIfTrueCondition, FirExpressionStub, FirLazyExpression, FirUnitExpression]
            Expression -> visitExpressionChildren(element as FirExpression, data)
            StatementStub -> visitStatementStubChildren(element as FirStatementStub, data)
            ContextReceiver -> visitContextReceiverChildren(element as FirContextReceiver, data)
            AnonymousInitializer -> visitAnonymousInitializerChildren(element as FirAnonymousInitializer, data)
            // TypeParameterRef -> [FirOuterClassTypeParameterRef, FirConstructedClassTypeParameterRef]
            TypeParameterRef -> visitTypeParameterRefChildren(element as FirTypeParameterRef, data)
            TypeParameter -> visitTypeParameterChildren(element as FirTypeParameter, data)
            ValueParameter -> visitValueParameterChildren(element as FirValueParameter, data)
            Property -> visitPropertyChildren(element as FirProperty, data)
            Field -> visitFieldChildren(element as FirField, data)
            EnumEntry -> visitEnumEntryChildren(element as FirEnumEntry, data)
            RegularClass -> visitRegularClassChildren(element as FirRegularClass, data)
            TypeAlias -> visitTypeAliasChildren(element as FirTypeAlias, data)
            SimpleFunction -> visitSimpleFunctionChildren(element as FirSimpleFunction, data)
            PropertyAccessor -> visitPropertyAccessorChildren(element as FirPropertyAccessor, data)
            BackingField -> visitBackingFieldChildren(element as FirBackingField, data)
            Constructor -> visitConstructorChildren(element as FirConstructor, data)
            File -> visitFileChildren(element as FirFile, data)
            PackageDirective -> visitPackageDirectiveChildren(element as FirPackageDirective, data)
            AnonymousFunction -> visitAnonymousFunctionChildren(element as FirAnonymousFunction, data)
            AnonymousFunctionExpression -> visitAnonymousFunctionExpressionChildren(element as FirAnonymousFunctionExpression, data)
            AnonymousObject -> visitAnonymousObjectChildren(element as FirAnonymousObject, data)
            AnonymousObjectExpression -> visitAnonymousObjectExpressionChildren(element as FirAnonymousObjectExpression, data)
            // Import -> [null]
            Import -> visitImportChildren(element as FirImport, data)
            ResolvedImport -> visitResolvedImportChildren(element as FirResolvedImport, data)
            ErrorImport -> visitErrorImportChildren(element as FirErrorImport, data)
            ErrorLoop -> visitErrorLoopChildren(element as FirErrorLoop, data)
            DoWhileLoop -> visitDoWhileLoopChildren(element as FirDoWhileLoop, data)
            WhileLoop -> visitWhileLoopChildren(element as FirWhileLoop, data)
            Block -> visitBlockChildren(element as FirBlock, data)
            BinaryLogicExpression -> visitBinaryLogicExpressionChildren(element as FirBinaryLogicExpression, data)
            BreakExpression -> visitBreakExpressionChildren(element as FirBreakExpression, data)
            ContinueExpression -> visitContinueExpressionChildren(element as FirContinueExpression, data)
            Catch -> visitCatchChildren(element as FirCatch, data)
            TryExpression -> visitTryExpressionChildren(element as FirTryExpression, data)
            ConstExpression -> visitConstExpressionChildren(element as FirConstExpression<*> , data)
            StarProjection -> visitStarProjectionChildren(element as FirStarProjection, data)
            PlaceholderProjection -> visitPlaceholderProjectionChildren(element as FirPlaceholderProjection, data)
            TypeProjectionWithVariance -> visitTypeProjectionWithVarianceChildren(element as FirTypeProjectionWithVariance, data)
            ArgumentList -> visitArgumentListChildren(element as FirArgumentList, data)
            // Annotation -> [null]
            Annotation -> visitAnnotationChildren(element as FirAnnotation, data)
            AnnotationCall -> visitAnnotationCallChildren(element as FirAnnotationCall, data)
            AnnotationArgumentMapping -> visitAnnotationArgumentMappingChildren(element as FirAnnotationArgumentMapping, data)
            ComparisonExpression -> visitComparisonExpressionChildren(element as FirComparisonExpression, data)
            TypeOperatorCall -> visitTypeOperatorCallChildren(element as FirTypeOperatorCall, data)
            AssignmentOperatorStatement -> visitAssignmentOperatorStatementChildren(element as FirAssignmentOperatorStatement, data)
            EqualityOperatorCall -> visitEqualityOperatorCallChildren(element as FirEqualityOperatorCall, data)
            WhenExpression -> visitWhenExpressionChildren(element as FirWhenExpression, data)
            WhenBranch -> visitWhenBranchChildren(element as FirWhenBranch, data)
            CheckNotNullCall -> visitCheckNotNullCallChildren(element as FirCheckNotNullCall, data)
            ElvisExpression -> visitElvisExpressionChildren(element as FirElvisExpression, data)
            ArrayOfCall -> visitArrayOfCallChildren(element as FirArrayOfCall, data)
            AugmentedArraySetCall -> visitAugmentedArraySetCallChildren(element as FirAugmentedArraySetCall, data)
            ClassReferenceExpression -> visitClassReferenceExpressionChildren(element as FirClassReferenceExpression, data)
            ErrorExpression -> visitErrorExpressionChildren(element as FirErrorExpression, data)
            ErrorFunction -> visitErrorFunctionChildren(element as FirErrorFunction, data)
            ErrorProperty -> visitErrorPropertyChildren(element as FirErrorProperty, data)
            PropertyAccessExpression -> visitPropertyAccessExpressionChildren(element as FirPropertyAccessExpression, data)
            // FunctionCall -> [null]
            FunctionCall -> visitFunctionCallChildren(element as FirFunctionCall, data)
            IntegerLiteralOperatorCall -> visitIntegerLiteralOperatorCallChildren(element as FirIntegerLiteralOperatorCall, data)
            ImplicitInvokeCall -> visitImplicitInvokeCallChildren(element as FirImplicitInvokeCall, data)
            DelegatedConstructorCall -> visitDelegatedConstructorCallChildren(element as FirDelegatedConstructorCall, data)
            ComponentCall -> visitComponentCallChildren(element as FirComponentCall, data)
            CallableReferenceAccess -> visitCallableReferenceAccessChildren(element as FirCallableReferenceAccess, data)
            ThisReceiverExpression -> visitThisReceiverExpressionChildren(element as FirThisReceiverExpression, data)
            SafeCallExpression -> visitSafeCallExpressionChildren(element as FirSafeCallExpression, data)
            CheckedSafeCallSubject -> visitCheckedSafeCallSubjectChildren(element as FirCheckedSafeCallSubject, data)
            GetClassCall -> visitGetClassCallChildren(element as FirGetClassCall, data)
            LambdaArgumentExpression -> visitLambdaArgumentExpressionChildren(element as FirLambdaArgumentExpression, data)
            SpreadArgumentExpression -> visitSpreadArgumentExpressionChildren(element as FirSpreadArgumentExpression, data)
            NamedArgumentExpression -> visitNamedArgumentExpressionChildren(element as FirNamedArgumentExpression, data)
            VarargArgumentsExpression -> visitVarargArgumentsExpressionChildren(element as FirVarargArgumentsExpression, data)
            // ResolvedQualifier -> [null]
            ResolvedQualifier -> visitResolvedQualifierChildren(element as FirResolvedQualifier, data)
            ErrorResolvedQualifier -> visitErrorResolvedQualifierChildren(element as FirErrorResolvedQualifier, data)
            ResolvedReifiedParameterReference -> visitResolvedReifiedParameterReferenceChildren(element as FirResolvedReifiedParameterReference, data)
            ReturnExpression -> visitReturnExpressionChildren(element as FirReturnExpression, data)
            StringConcatenationCall -> visitStringConcatenationCallChildren(element as FirStringConcatenationCall, data)
            ThrowExpression -> visitThrowExpressionChildren(element as FirThrowExpression, data)
            VariableAssignment -> visitVariableAssignmentChildren(element as FirVariableAssignment, data)
            WhenSubjectExpression -> visitWhenSubjectExpressionChildren(element as FirWhenSubjectExpression, data)
            WrappedDelegateExpression -> visitWrappedDelegateExpressionChildren(element as FirWrappedDelegateExpression, data)
            // NamedReference -> [FirSimpleNamedReference]
            NamedReference -> visitNamedReferenceChildren(element as FirNamedReference, data)
            ErrorNamedReference -> visitErrorNamedReferenceChildren(element as FirErrorNamedReference, data)
            SuperReference -> visitSuperReferenceChildren(element as FirSuperReference, data)
            ThisReference -> visitThisReferenceChildren(element as FirThisReference, data)
            ControlFlowGraphReference -> visitControlFlowGraphReferenceChildren(element as FirControlFlowGraphReference, data)
            // ResolvedNamedReference -> [FirPropertyFromParameterResolvedNamedReference, null]
            ResolvedNamedReference -> visitResolvedNamedReferenceChildren(element as FirResolvedNamedReference, data)
            DelegateFieldReference -> visitDelegateFieldReferenceChildren(element as FirDelegateFieldReference, data)
            BackingFieldReference -> visitBackingFieldReferenceChildren(element as FirBackingFieldReference, data)
            ResolvedCallableReference -> visitResolvedCallableReferenceChildren(element as FirResolvedCallableReference, data)
            // ResolvedTypeRef -> [null]
            ResolvedTypeRef -> visitResolvedTypeRefChildren(element as FirResolvedTypeRef, data)
            ErrorTypeRef -> visitErrorTypeRefChildren(element as FirErrorTypeRef, data)
            UserTypeRef -> visitUserTypeRefChildren(element as FirUserTypeRef, data)
            DynamicTypeRef -> visitDynamicTypeRefChildren(element as FirDynamicTypeRef, data)
            FunctionTypeRef -> visitFunctionTypeRefChildren(element as FirFunctionTypeRef, data)
            IntersectionTypeRef -> visitIntersectionTypeRefChildren(element as FirIntersectionTypeRef, data)
            ImplicitTypeRef -> visitImplicitTypeRefChildren(element as FirImplicitTypeRef, data)
            SmartCastedTypeRef -> visitSmartCastedTypeRefChildren(element as FirSmartCastedTypeRef, data)
            EffectDeclaration -> visitEffectDeclarationChildren(element as FirEffectDeclaration, data)
            ContractDescription -> visitContractDescriptionChildren(element as FirContractDescription, data)
            LegacyRawContractDescription -> visitLegacyRawContractDescriptionChildren(element as FirLegacyRawContractDescription, data)
            RawContractDescription -> visitRawContractDescriptionChildren(element as FirRawContractDescription, data)
            ResolvedContractDescription -> visitResolvedContractDescriptionChildren(element as FirResolvedContractDescription, data)
        }
    }
    open fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: D): R  = visitElement(annotationContainer, data)

    private fun visitAnnotationContainerChildren(annotationContainer: FirAnnotationContainer, data: D) {
        annotationContainer.annotations.forEach { it.accept(this, data) }
    }

    open fun visitTypeRef(typeRef: FirTypeRef, data: D): R  = visitElement(typeRef, data)

    private fun visitTypeRefChildren(typeRef: FirTypeRef, data: D) {
        typeRef.annotations.forEach { it.accept(this, data) }
    }

    open fun visitReference(reference: FirReference, data: D): R  = visitElement(reference, data)

    private fun visitReferenceChildren(reference: FirReference, data: D) {
    }

    open fun visitLabel(label: FirLabel, data: D): R  = visitElement(label, data)

    private fun visitLabelChildren(label: FirLabel, data: D) {
    }

    open fun visitResolvable(resolvable: FirResolvable, data: D): R  = visitElement(resolvable, data)

    private fun visitResolvableChildren(resolvable: FirResolvable, data: D) {
        resolvable.calleeReference.accept(this, data)
    }

    open fun visitTargetElement(targetElement: FirTargetElement, data: D): R  = visitElement(targetElement, data)

    private fun visitTargetElementChildren(targetElement: FirTargetElement, data: D) {
    }

    open fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus, data: D): R  = visitElement(declarationStatus, data)

    private fun visitDeclarationStatusChildren(declarationStatus: FirDeclarationStatus, data: D) {
    }

    open fun visitResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: D): R  = visitElement(resolvedDeclarationStatus, data)

    private fun visitResolvedDeclarationStatusChildren(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: D) {
    }

    open fun visitControlFlowGraphOwner(controlFlowGraphOwner: FirControlFlowGraphOwner, data: D): R  = visitElement(controlFlowGraphOwner, data)

    private fun visitControlFlowGraphOwnerChildren(controlFlowGraphOwner: FirControlFlowGraphOwner, data: D) {
        controlFlowGraphOwner.controlFlowGraphReference?.accept(this, data)
    }

    open fun visitStatement(statement: FirStatement, data: D): R  = visitElement(statement, data)

    private fun visitStatementChildren(statement: FirStatement, data: D) {
        statement.annotations.forEach { it.accept(this, data) }
    }

    open fun visitExpression(expression: FirExpression, data: D): R  = visitElement(expression, data)

    private fun visitExpressionChildren(expression: FirExpression, data: D) {
        expression.typeRef.accept(this, data)
        expression.annotations.forEach { it.accept(this, data) }
    }

    open fun visitStatementStub(statementStub: FirStatementStub, data: D): R  = visitElement(statementStub, data)

    private fun visitStatementStubChildren(statementStub: FirStatementStub, data: D) {
        statementStub.annotations.forEach { it.accept(this, data) }
    }

    open fun visitContextReceiver(contextReceiver: FirContextReceiver, data: D): R  = visitElement(contextReceiver, data)

    private fun visitContextReceiverChildren(contextReceiver: FirContextReceiver, data: D) {
        contextReceiver.typeRef.accept(this, data)
    }

    open fun visitDeclaration(declaration: FirDeclaration, data: D): R  = visitElement(declaration, data)

    private fun visitDeclarationChildren(declaration: FirDeclaration, data: D) {
        declaration.annotations.forEach { it.accept(this, data) }
    }

    open fun visitTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: D): R  = visitElement(typeParameterRefsOwner, data)

    private fun visitTypeParameterRefsOwnerChildren(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: D) {
        typeParameterRefsOwner.typeParameters.forEach { it.accept(this, data) }
    }

    open fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): R  = visitElement(memberDeclaration, data)

    private fun visitMemberDeclarationChildren(memberDeclaration: FirMemberDeclaration, data: D) {
        memberDeclaration.annotations.forEach { it.accept(this, data) }
        memberDeclaration.typeParameters.forEach { it.accept(this, data) }
        memberDeclaration.status.accept(this, data)
    }

    open fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: D): R  = visitElement(anonymousInitializer, data)

    private fun visitAnonymousInitializerChildren(anonymousInitializer: FirAnonymousInitializer, data: D) {
        anonymousInitializer.annotations.forEach { it.accept(this, data) }
        anonymousInitializer.controlFlowGraphReference?.accept(this, data)
        anonymousInitializer.body?.accept(this, data)
    }

    open fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: D): R  = visitElement(callableDeclaration, data)

    private fun visitCallableDeclarationChildren(callableDeclaration: FirCallableDeclaration, data: D) {
        callableDeclaration.annotations.forEach { it.accept(this, data) }
        callableDeclaration.typeParameters.forEach { it.accept(this, data) }
        callableDeclaration.status.accept(this, data)
        callableDeclaration.returnTypeRef.accept(this, data)
        callableDeclaration.receiverTypeRef?.accept(this, data)
        callableDeclaration.contextReceivers.forEach { it.accept(this, data) }
    }

    open fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef, data: D): R  = visitElement(typeParameterRef, data)

    private fun visitTypeParameterRefChildren(typeParameterRef: FirTypeParameterRef, data: D) {
    }

    open fun visitTypeParameter(typeParameter: FirTypeParameter, data: D): R  = visitElement(typeParameter, data)

    private fun visitTypeParameterChildren(typeParameter: FirTypeParameter, data: D) {
        typeParameter.bounds.forEach { it.accept(this, data) }
        typeParameter.annotations.forEach { it.accept(this, data) }
    }

    open fun visitVariable(variable: FirVariable, data: D): R  = visitElement(variable, data)

    private fun visitVariableChildren(variable: FirVariable, data: D) {
        variable.typeParameters.forEach { it.accept(this, data) }
        variable.status.accept(this, data)
        variable.returnTypeRef.accept(this, data)
        variable.receiverTypeRef?.accept(this, data)
        variable.contextReceivers.forEach { it.accept(this, data) }
        variable.initializer?.accept(this, data)
        variable.delegate?.accept(this, data)
        variable.getter?.accept(this, data)
        variable.setter?.accept(this, data)
        variable.backingField?.accept(this, data)
        variable.annotations.forEach { it.accept(this, data) }
    }

    open fun visitValueParameter(valueParameter: FirValueParameter, data: D): R  = visitElement(valueParameter, data)

    private fun visitValueParameterChildren(valueParameter: FirValueParameter, data: D) {
        valueParameter.typeParameters.forEach { it.accept(this, data) }
        valueParameter.status.accept(this, data)
        valueParameter.returnTypeRef.accept(this, data)
        valueParameter.receiverTypeRef?.accept(this, data)
        valueParameter.contextReceivers.forEach { it.accept(this, data) }
        valueParameter.initializer?.accept(this, data)
        valueParameter.delegate?.accept(this, data)
        valueParameter.getter?.accept(this, data)
        valueParameter.setter?.accept(this, data)
        valueParameter.backingField?.accept(this, data)
        valueParameter.annotations.forEach { it.accept(this, data) }
        valueParameter.controlFlowGraphReference?.accept(this, data)
        valueParameter.defaultValue?.accept(this, data)
    }

    open fun visitProperty(property: FirProperty, data: D): R  = visitElement(property, data)

    private fun visitPropertyChildren(property: FirProperty, data: D) {
        property.status.accept(this, data)
        property.returnTypeRef.accept(this, data)
        property.receiverTypeRef?.accept(this, data)
        property.contextReceivers.forEach { it.accept(this, data) }
        property.initializer?.accept(this, data)
        property.delegate?.accept(this, data)
        property.getter?.accept(this, data)
        property.setter?.accept(this, data)
        property.backingField?.accept(this, data)
        property.annotations.forEach { it.accept(this, data) }
        property.controlFlowGraphReference?.accept(this, data)
        property.typeParameters.forEach { it.accept(this, data) }
    }

    open fun visitField(field: FirField, data: D): R  = visitElement(field, data)

    private fun visitFieldChildren(field: FirField, data: D) {
        field.typeParameters.forEach { it.accept(this, data) }
        field.status.accept(this, data)
        field.returnTypeRef.accept(this, data)
        field.receiverTypeRef?.accept(this, data)
        field.contextReceivers.forEach { it.accept(this, data) }
        field.initializer?.accept(this, data)
        field.delegate?.accept(this, data)
        field.getter?.accept(this, data)
        field.setter?.accept(this, data)
        field.backingField?.accept(this, data)
        field.annotations.forEach { it.accept(this, data) }
        field.controlFlowGraphReference?.accept(this, data)
    }

    open fun visitEnumEntry(enumEntry: FirEnumEntry, data: D): R  = visitElement(enumEntry, data)

    private fun visitEnumEntryChildren(enumEntry: FirEnumEntry, data: D) {
        enumEntry.typeParameters.forEach { it.accept(this, data) }
        enumEntry.status.accept(this, data)
        enumEntry.returnTypeRef.accept(this, data)
        enumEntry.receiverTypeRef?.accept(this, data)
        enumEntry.contextReceivers.forEach { it.accept(this, data) }
        enumEntry.initializer?.accept(this, data)
        enumEntry.delegate?.accept(this, data)
        enumEntry.getter?.accept(this, data)
        enumEntry.setter?.accept(this, data)
        enumEntry.backingField?.accept(this, data)
        enumEntry.annotations.forEach { it.accept(this, data) }
    }

    open fun visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration, data: D): R  = visitElement(classLikeDeclaration, data)

    private fun visitClassLikeDeclarationChildren(classLikeDeclaration: FirClassLikeDeclaration, data: D) {
        classLikeDeclaration.annotations.forEach { it.accept(this, data) }
        classLikeDeclaration.typeParameters.forEach { it.accept(this, data) }
        classLikeDeclaration.status.accept(this, data)
    }

    open fun visitClass(klass: FirClass, data: D): R  = visitElement(klass, data)

    private fun visitClassChildren(klass: FirClass, data: D) {
        klass.typeParameters.forEach { it.accept(this, data) }
        klass.status.accept(this, data)
        klass.superTypeRefs.forEach { it.accept(this, data) }
        klass.declarations.forEach { it.accept(this, data) }
        klass.annotations.forEach { it.accept(this, data) }
    }

    open fun visitRegularClass(regularClass: FirRegularClass, data: D): R  = visitElement(regularClass, data)

    private fun visitRegularClassChildren(regularClass: FirRegularClass, data: D) {
        regularClass.typeParameters.forEach { it.accept(this, data) }
        regularClass.status.accept(this, data)
        regularClass.declarations.forEach { it.accept(this, data) }
        regularClass.annotations.forEach { it.accept(this, data) }
        regularClass.controlFlowGraphReference?.accept(this, data)
        regularClass.superTypeRefs.forEach { it.accept(this, data) }
        regularClass.contextReceivers.forEach { it.accept(this, data) }
    }

    open fun visitTypeAlias(typeAlias: FirTypeAlias, data: D): R  = visitElement(typeAlias, data)

    private fun visitTypeAliasChildren(typeAlias: FirTypeAlias, data: D) {
        typeAlias.status.accept(this, data)
        typeAlias.typeParameters.forEach { it.accept(this, data) }
        typeAlias.expandedTypeRef.accept(this, data)
        typeAlias.annotations.forEach { it.accept(this, data) }
    }

    open fun visitFunction(function: FirFunction, data: D): R  = visitElement(function, data)

    private fun visitFunctionChildren(function: FirFunction, data: D) {
        function.annotations.forEach { it.accept(this, data) }
        function.typeParameters.forEach { it.accept(this, data) }
        function.status.accept(this, data)
        function.returnTypeRef.accept(this, data)
        function.receiverTypeRef?.accept(this, data)
        function.contextReceivers.forEach { it.accept(this, data) }
        function.controlFlowGraphReference?.accept(this, data)
        function.valueParameters.forEach { it.accept(this, data) }
        function.body?.accept(this, data)
    }

    open fun visitContractDescriptionOwner(contractDescriptionOwner: FirContractDescriptionOwner, data: D): R  = visitElement(contractDescriptionOwner, data)

    private fun visitContractDescriptionOwnerChildren(contractDescriptionOwner: FirContractDescriptionOwner, data: D) {
        contractDescriptionOwner.contractDescription.accept(this, data)
    }

    open fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: D): R  = visitElement(simpleFunction, data)

    private fun visitSimpleFunctionChildren(simpleFunction: FirSimpleFunction, data: D) {
        simpleFunction.status.accept(this, data)
        simpleFunction.returnTypeRef.accept(this, data)
        simpleFunction.receiverTypeRef?.accept(this, data)
        simpleFunction.contextReceivers.forEach { it.accept(this, data) }
        simpleFunction.controlFlowGraphReference?.accept(this, data)
        simpleFunction.valueParameters.forEach { it.accept(this, data) }
        simpleFunction.body?.accept(this, data)
        simpleFunction.contractDescription.accept(this, data)
        simpleFunction.annotations.forEach { it.accept(this, data) }
        simpleFunction.typeParameters.forEach { it.accept(this, data) }
    }

    open fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): R  = visitElement(propertyAccessor, data)

    private fun visitPropertyAccessorChildren(propertyAccessor: FirPropertyAccessor, data: D) {
        propertyAccessor.status.accept(this, data)
        propertyAccessor.returnTypeRef.accept(this, data)
        propertyAccessor.receiverTypeRef?.accept(this, data)
        propertyAccessor.contextReceivers.forEach { it.accept(this, data) }
        propertyAccessor.controlFlowGraphReference?.accept(this, data)
        propertyAccessor.valueParameters.forEach { it.accept(this, data) }
        propertyAccessor.body?.accept(this, data)
        propertyAccessor.contractDescription.accept(this, data)
        propertyAccessor.annotations.forEach { it.accept(this, data) }
        propertyAccessor.typeParameters.forEach { it.accept(this, data) }
    }

    open fun visitBackingField(backingField: FirBackingField, data: D): R  = visitElement(backingField, data)

    private fun visitBackingFieldChildren(backingField: FirBackingField, data: D) {
        backingField.returnTypeRef.accept(this, data)
        backingField.receiverTypeRef?.accept(this, data)
        backingField.contextReceivers.forEach { it.accept(this, data) }
        backingField.delegate?.accept(this, data)
        backingField.getter?.accept(this, data)
        backingField.setter?.accept(this, data)
        backingField.backingField?.accept(this, data)
        backingField.initializer?.accept(this, data)
        backingField.annotations.forEach { it.accept(this, data) }
        backingField.typeParameters.forEach { it.accept(this, data) }
        backingField.status.accept(this, data)
    }

    open fun visitConstructor(constructor: FirConstructor, data: D): R  = visitElement(constructor, data)

    private fun visitConstructorChildren(constructor: FirConstructor, data: D) {
        constructor.typeParameters.forEach { it.accept(this, data) }
        constructor.status.accept(this, data)
        constructor.returnTypeRef.accept(this, data)
        constructor.receiverTypeRef?.accept(this, data)
        constructor.contextReceivers.forEach { it.accept(this, data) }
        constructor.controlFlowGraphReference?.accept(this, data)
        constructor.valueParameters.forEach { it.accept(this, data) }
        constructor.annotations.forEach { it.accept(this, data) }
        constructor.delegatedConstructor?.accept(this, data)
        constructor.body?.accept(this, data)
    }

    open fun visitFile(file: FirFile, data: D): R  = visitElement(file, data)

    private fun visitFileChildren(file: FirFile, data: D) {
        file.annotations.forEach { it.accept(this, data) }
        file.packageDirective.accept(this, data)
        file.imports.forEach { it.accept(this, data) }
        file.declarations.forEach { it.accept(this, data) }
    }

    open fun visitPackageDirective(packageDirective: FirPackageDirective, data: D): R  = visitElement(packageDirective, data)

    private fun visitPackageDirectiveChildren(packageDirective: FirPackageDirective, data: D) {
    }

    open fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: D): R  = visitElement(anonymousFunction, data)

    private fun visitAnonymousFunctionChildren(anonymousFunction: FirAnonymousFunction, data: D) {
        anonymousFunction.annotations.forEach { it.accept(this, data) }
        anonymousFunction.status.accept(this, data)
        anonymousFunction.returnTypeRef.accept(this, data)
        anonymousFunction.receiverTypeRef?.accept(this, data)
        anonymousFunction.contextReceivers.forEach { it.accept(this, data) }
        anonymousFunction.controlFlowGraphReference?.accept(this, data)
        anonymousFunction.valueParameters.forEach { it.accept(this, data) }
        anonymousFunction.body?.accept(this, data)
        anonymousFunction.label?.accept(this, data)
        anonymousFunction.typeParameters.forEach { it.accept(this, data) }
        anonymousFunction.typeRef.accept(this, data)
    }

    open fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: D): R  = visitElement(anonymousFunctionExpression, data)

    private fun visitAnonymousFunctionExpressionChildren(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: D) {
        anonymousFunctionExpression.typeRef.accept(this, data)
        anonymousFunctionExpression.annotations.forEach { it.accept(this, data) }
        anonymousFunctionExpression.anonymousFunction.accept(this, data)
    }

    open fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: D): R  = visitElement(anonymousObject, data)

    private fun visitAnonymousObjectChildren(anonymousObject: FirAnonymousObject, data: D) {
        anonymousObject.typeParameters.forEach { it.accept(this, data) }
        anonymousObject.status.accept(this, data)
        anonymousObject.superTypeRefs.forEach { it.accept(this, data) }
        anonymousObject.declarations.forEach { it.accept(this, data) }
        anonymousObject.annotations.forEach { it.accept(this, data) }
        anonymousObject.controlFlowGraphReference?.accept(this, data)
    }

    open fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: D): R  = visitElement(anonymousObjectExpression, data)

    private fun visitAnonymousObjectExpressionChildren(anonymousObjectExpression: FirAnonymousObjectExpression, data: D) {
        anonymousObjectExpression.typeRef.accept(this, data)
        anonymousObjectExpression.annotations.forEach { it.accept(this, data) }
        anonymousObjectExpression.anonymousObject.accept(this, data)
    }

    open fun visitDiagnosticHolder(diagnosticHolder: FirDiagnosticHolder, data: D): R  = visitElement(diagnosticHolder, data)

    private fun visitDiagnosticHolderChildren(diagnosticHolder: FirDiagnosticHolder, data: D) {
    }

    open fun visitImport(import: FirImport, data: D): R  = visitElement(import, data)

    private fun visitImportChildren(import: FirImport, data: D) {
    }

    open fun visitResolvedImport(resolvedImport: FirResolvedImport, data: D): R  = visitElement(resolvedImport, data)

    private fun visitResolvedImportChildren(resolvedImport: FirResolvedImport, data: D) {
        resolvedImport.delegate.accept(this, data)
    }

    open fun visitErrorImport(errorImport: FirErrorImport, data: D): R  = visitElement(errorImport, data)

    private fun visitErrorImportChildren(errorImport: FirErrorImport, data: D) {
        errorImport.delegate.accept(this, data)
    }

    open fun visitLoop(loop: FirLoop, data: D): R  = visitElement(loop, data)

    private fun visitLoopChildren(loop: FirLoop, data: D) {
        loop.annotations.forEach { it.accept(this, data) }
        loop.block.accept(this, data)
        loop.condition.accept(this, data)
        loop.label?.accept(this, data)
    }

    open fun visitErrorLoop(errorLoop: FirErrorLoop, data: D): R  = visitElement(errorLoop, data)

    private fun visitErrorLoopChildren(errorLoop: FirErrorLoop, data: D) {
        errorLoop.annotations.forEach { it.accept(this, data) }
        errorLoop.block.accept(this, data)
        errorLoop.condition.accept(this, data)
        errorLoop.label?.accept(this, data)
    }

    open fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: D): R  = visitElement(doWhileLoop, data)

    private fun visitDoWhileLoopChildren(doWhileLoop: FirDoWhileLoop, data: D) {
        doWhileLoop.annotations.forEach { it.accept(this, data) }
        doWhileLoop.block.accept(this, data)
        doWhileLoop.condition.accept(this, data)
        doWhileLoop.label?.accept(this, data)
    }

    open fun visitWhileLoop(whileLoop: FirWhileLoop, data: D): R  = visitElement(whileLoop, data)

    private fun visitWhileLoopChildren(whileLoop: FirWhileLoop, data: D) {
        whileLoop.annotations.forEach { it.accept(this, data) }
        whileLoop.label?.accept(this, data)
        whileLoop.condition.accept(this, data)
        whileLoop.block.accept(this, data)
    }

    open fun visitBlock(block: FirBlock, data: D): R  = visitElement(block, data)

    private fun visitBlockChildren(block: FirBlock, data: D) {
        block.annotations.forEach { it.accept(this, data) }
        block.statements.forEach { it.accept(this, data) }
        block.typeRef.accept(this, data)
    }

    open fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: D): R  = visitElement(binaryLogicExpression, data)

    private fun visitBinaryLogicExpressionChildren(binaryLogicExpression: FirBinaryLogicExpression, data: D) {
        binaryLogicExpression.typeRef.accept(this, data)
        binaryLogicExpression.annotations.forEach { it.accept(this, data) }
        binaryLogicExpression.leftOperand.accept(this, data)
        binaryLogicExpression.rightOperand.accept(this, data)
    }

    open fun <E : FirTargetElement> visitJump(jump: FirJump<E>, data: D): R  = visitElement(jump, data)

    private fun <E : FirTargetElement> visitJumpChildren(jump: FirJump<E>, data: D) {
        jump.typeRef.accept(this, data)
        jump.annotations.forEach { it.accept(this, data) }
    }

    open fun visitLoopJump(loopJump: FirLoopJump, data: D): R  = visitElement(loopJump, data)

    private fun visitLoopJumpChildren(loopJump: FirLoopJump, data: D) {
        loopJump.typeRef.accept(this, data)
        loopJump.annotations.forEach { it.accept(this, data) }
    }

    open fun visitBreakExpression(breakExpression: FirBreakExpression, data: D): R  = visitElement(breakExpression, data)

    private fun visitBreakExpressionChildren(breakExpression: FirBreakExpression, data: D) {
        breakExpression.typeRef.accept(this, data)
        breakExpression.annotations.forEach { it.accept(this, data) }
    }

    open fun visitContinueExpression(continueExpression: FirContinueExpression, data: D): R  = visitElement(continueExpression, data)

    private fun visitContinueExpressionChildren(continueExpression: FirContinueExpression, data: D) {
        continueExpression.typeRef.accept(this, data)
        continueExpression.annotations.forEach { it.accept(this, data) }
    }

    open fun visitCatch(catch: FirCatch, data: D): R  = visitElement(catch, data)

    private fun visitCatchChildren(catch: FirCatch, data: D) {
        catch.parameter.accept(this, data)
        catch.block.accept(this, data)
    }

    open fun visitTryExpression(tryExpression: FirTryExpression, data: D): R  = visitElement(tryExpression, data)

    private fun visitTryExpressionChildren(tryExpression: FirTryExpression, data: D) {
        tryExpression.typeRef.accept(this, data)
        tryExpression.annotations.forEach { it.accept(this, data) }
        tryExpression.calleeReference.accept(this, data)
        tryExpression.tryBlock.accept(this, data)
        tryExpression.catches.forEach { it.accept(this, data) }
        tryExpression.finallyBlock?.accept(this, data)
    }

    open fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: D): R  = visitElement(constExpression, data)

    private fun <T> visitConstExpressionChildren(constExpression: FirConstExpression<T>, data: D) {
        constExpression.typeRef.accept(this, data)
        constExpression.annotations.forEach { it.accept(this, data) }
    }

    open fun visitTypeProjection(typeProjection: FirTypeProjection, data: D): R  = visitElement(typeProjection, data)

    private fun visitTypeProjectionChildren(typeProjection: FirTypeProjection, data: D) {
    }

    open fun visitStarProjection(starProjection: FirStarProjection, data: D): R  = visitElement(starProjection, data)

    private fun visitStarProjectionChildren(starProjection: FirStarProjection, data: D) {
    }

    open fun visitPlaceholderProjection(placeholderProjection: FirPlaceholderProjection, data: D): R  = visitElement(placeholderProjection, data)

    private fun visitPlaceholderProjectionChildren(placeholderProjection: FirPlaceholderProjection, data: D) {
    }

    open fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): R  = visitElement(typeProjectionWithVariance, data)

    private fun visitTypeProjectionWithVarianceChildren(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D) {
        typeProjectionWithVariance.typeRef.accept(this, data)
    }

    open fun visitArgumentList(argumentList: FirArgumentList, data: D): R  = visitElement(argumentList, data)

    private fun visitArgumentListChildren(argumentList: FirArgumentList, data: D) {
        argumentList.arguments.forEach { it.accept(this, data) }
    }

    open fun visitCall(call: FirCall, data: D): R  = visitElement(call, data)

    private fun visitCallChildren(call: FirCall, data: D) {
        call.annotations.forEach { it.accept(this, data) }
        call.argumentList.accept(this, data)
    }

    open fun visitAnnotation(annotation: FirAnnotation, data: D): R  = visitElement(annotation, data)

    private fun visitAnnotationChildren(annotation: FirAnnotation, data: D) {
        annotation.typeRef.accept(this, data)
        annotation.annotations.forEach { it.accept(this, data) }
        annotation.annotationTypeRef.accept(this, data)
        annotation.argumentMapping.accept(this, data)
        annotation.typeArguments.forEach { it.accept(this, data) }
    }

    open fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: D): R  = visitElement(annotationCall, data)

    private fun visitAnnotationCallChildren(annotationCall: FirAnnotationCall, data: D) {
        annotationCall.typeRef.accept(this, data)
        annotationCall.annotations.forEach { it.accept(this, data) }
        annotationCall.annotationTypeRef.accept(this, data)
        annotationCall.typeArguments.forEach { it.accept(this, data) }
        annotationCall.argumentList.accept(this, data)
        annotationCall.calleeReference.accept(this, data)
        annotationCall.argumentMapping.accept(this, data)
    }

    open fun visitAnnotationArgumentMapping(annotationArgumentMapping: FirAnnotationArgumentMapping, data: D): R  = visitElement(annotationArgumentMapping, data)

    private fun visitAnnotationArgumentMappingChildren(annotationArgumentMapping: FirAnnotationArgumentMapping, data: D) {
    }

    open fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: D): R  = visitElement(comparisonExpression, data)

    private fun visitComparisonExpressionChildren(comparisonExpression: FirComparisonExpression, data: D) {
        comparisonExpression.typeRef.accept(this, data)
        comparisonExpression.annotations.forEach { it.accept(this, data) }
        comparisonExpression.compareToCall.accept(this, data)
    }

    open fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: D): R  = visitElement(typeOperatorCall, data)

    private fun visitTypeOperatorCallChildren(typeOperatorCall: FirTypeOperatorCall, data: D) {
        typeOperatorCall.typeRef.accept(this, data)
        typeOperatorCall.annotations.forEach { it.accept(this, data) }
        typeOperatorCall.argumentList.accept(this, data)
        typeOperatorCall.conversionTypeRef.accept(this, data)
    }

    open fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: D): R  = visitElement(assignmentOperatorStatement, data)

    private fun visitAssignmentOperatorStatementChildren(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: D) {
        assignmentOperatorStatement.annotations.forEach { it.accept(this, data) }
        assignmentOperatorStatement.leftArgument.accept(this, data)
        assignmentOperatorStatement.rightArgument.accept(this, data)
    }

    open fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: D): R  = visitElement(equalityOperatorCall, data)

    private fun visitEqualityOperatorCallChildren(equalityOperatorCall: FirEqualityOperatorCall, data: D) {
        equalityOperatorCall.typeRef.accept(this, data)
        equalityOperatorCall.annotations.forEach { it.accept(this, data) }
        equalityOperatorCall.argumentList.accept(this, data)
    }

    open fun visitWhenExpression(whenExpression: FirWhenExpression, data: D): R  = visitElement(whenExpression, data)

    private fun visitWhenExpressionChildren(whenExpression: FirWhenExpression, data: D) {
        whenExpression.typeRef.accept(this, data)
        whenExpression.annotations.forEach { it.accept(this, data) }
        whenExpression.calleeReference.accept(this, data)
        val subjectVariable_ = whenExpression.subjectVariable
        if (subjectVariable_ != null) {
            subjectVariable_.accept(this, data)
        } else {
            whenExpression.subject?.accept(this, data)
        }
        whenExpression.branches.forEach { it.accept(this, data) }
    }

    open fun visitWhenBranch(whenBranch: FirWhenBranch, data: D): R  = visitElement(whenBranch, data)

    private fun visitWhenBranchChildren(whenBranch: FirWhenBranch, data: D) {
        whenBranch.condition.accept(this, data)
        whenBranch.result.accept(this, data)
    }

    open fun visitContextReceiverArgumentListOwner(contextReceiverArgumentListOwner: FirContextReceiverArgumentListOwner, data: D): R  = visitElement(contextReceiverArgumentListOwner, data)

    private fun visitContextReceiverArgumentListOwnerChildren(contextReceiverArgumentListOwner: FirContextReceiverArgumentListOwner, data: D) {
        contextReceiverArgumentListOwner.contextReceiverArguments.forEach { it.accept(this, data) }
    }

    open fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccess, data: D): R  = visitElement(qualifiedAccess, data)

    private fun visitQualifiedAccessChildren(qualifiedAccess: FirQualifiedAccess, data: D) {
        qualifiedAccess.calleeReference.accept(this, data)
        qualifiedAccess.annotations.forEach { it.accept(this, data) }
        qualifiedAccess.contextReceiverArguments.forEach { it.accept(this, data) }
        qualifiedAccess.typeArguments.forEach { it.accept(this, data) }
        qualifiedAccess.explicitReceiver?.accept(this, data)
    }

    open fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: D): R  = visitElement(checkNotNullCall, data)

    private fun visitCheckNotNullCallChildren(checkNotNullCall: FirCheckNotNullCall, data: D) {
        checkNotNullCall.typeRef.accept(this, data)
        checkNotNullCall.annotations.forEach { it.accept(this, data) }
        checkNotNullCall.argumentList.accept(this, data)
        checkNotNullCall.calleeReference.accept(this, data)
    }

    open fun visitElvisExpression(elvisExpression: FirElvisExpression, data: D): R  = visitElement(elvisExpression, data)

    private fun visitElvisExpressionChildren(elvisExpression: FirElvisExpression, data: D) {
        elvisExpression.typeRef.accept(this, data)
        elvisExpression.annotations.forEach { it.accept(this, data) }
        elvisExpression.calleeReference.accept(this, data)
        elvisExpression.lhs.accept(this, data)
        elvisExpression.rhs.accept(this, data)
    }

    open fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: D): R  = visitElement(arrayOfCall, data)

    private fun visitArrayOfCallChildren(arrayOfCall: FirArrayOfCall, data: D) {
        arrayOfCall.typeRef.accept(this, data)
        arrayOfCall.annotations.forEach { it.accept(this, data) }
        arrayOfCall.argumentList.accept(this, data)
    }

    open fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall, data: D): R  = visitElement(augmentedArraySetCall, data)

    private fun visitAugmentedArraySetCallChildren(augmentedArraySetCall: FirAugmentedArraySetCall, data: D) {
        augmentedArraySetCall.annotations.forEach { it.accept(this, data) }
        augmentedArraySetCall.lhsGetCall.accept(this, data)
        augmentedArraySetCall.rhs.accept(this, data)
        augmentedArraySetCall.calleeReference.accept(this, data)
    }

    open fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: D): R  = visitElement(classReferenceExpression, data)

    private fun visitClassReferenceExpressionChildren(classReferenceExpression: FirClassReferenceExpression, data: D) {
        classReferenceExpression.typeRef.accept(this, data)
        classReferenceExpression.annotations.forEach { it.accept(this, data) }
        classReferenceExpression.classTypeRef.accept(this, data)
    }

    open fun visitErrorExpression(errorExpression: FirErrorExpression, data: D): R  = visitElement(errorExpression, data)

    private fun visitErrorExpressionChildren(errorExpression: FirErrorExpression, data: D) {
        errorExpression.typeRef.accept(this, data)
        errorExpression.annotations.forEach { it.accept(this, data) }
        errorExpression.expression?.accept(this, data)
    }

    open fun visitErrorFunction(errorFunction: FirErrorFunction, data: D): R  = visitElement(errorFunction, data)

    private fun visitErrorFunctionChildren(errorFunction: FirErrorFunction, data: D) {
        errorFunction.annotations.forEach { it.accept(this, data) }
        errorFunction.status.accept(this, data)
        errorFunction.returnTypeRef.accept(this, data)
        errorFunction.receiverTypeRef?.accept(this, data)
        errorFunction.contextReceivers.forEach { it.accept(this, data) }
        errorFunction.controlFlowGraphReference?.accept(this, data)
        errorFunction.valueParameters.forEach { it.accept(this, data) }
        errorFunction.body?.accept(this, data)
        errorFunction.typeParameters.forEach { it.accept(this, data) }
    }

    open fun visitErrorProperty(errorProperty: FirErrorProperty, data: D): R  = visitElement(errorProperty, data)

    private fun visitErrorPropertyChildren(errorProperty: FirErrorProperty, data: D) {
        errorProperty.typeParameters.forEach { it.accept(this, data) }
        errorProperty.status.accept(this, data)
        errorProperty.returnTypeRef.accept(this, data)
        errorProperty.receiverTypeRef?.accept(this, data)
        errorProperty.contextReceivers.forEach { it.accept(this, data) }
        errorProperty.initializer?.accept(this, data)
        errorProperty.delegate?.accept(this, data)
        errorProperty.getter?.accept(this, data)
        errorProperty.setter?.accept(this, data)
        errorProperty.backingField?.accept(this, data)
        errorProperty.annotations.forEach { it.accept(this, data) }
    }

    open fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D): R  = visitElement(qualifiedAccessExpression, data)

    private fun visitQualifiedAccessExpressionChildren(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D) {
        qualifiedAccessExpression.typeRef.accept(this, data)
        qualifiedAccessExpression.annotations.forEach { it.accept(this, data) }
        qualifiedAccessExpression.calleeReference.accept(this, data)
        qualifiedAccessExpression.contextReceiverArguments.forEach { it.accept(this, data) }
        qualifiedAccessExpression.typeArguments.forEach { it.accept(this, data) }
        qualifiedAccessExpression.explicitReceiver?.accept(this, data)
    }

    open fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: D): R  = visitElement(propertyAccessExpression, data)

    private fun visitPropertyAccessExpressionChildren(propertyAccessExpression: FirPropertyAccessExpression, data: D) {
        propertyAccessExpression.typeRef.accept(this, data)
        propertyAccessExpression.annotations.forEach { it.accept(this, data) }
        propertyAccessExpression.calleeReference.accept(this, data)
        propertyAccessExpression.contextReceiverArguments.forEach { it.accept(this, data) }
        propertyAccessExpression.typeArguments.forEach { it.accept(this, data) }
        propertyAccessExpression.explicitReceiver?.accept(this, data)
    }

    open fun visitFunctionCall(functionCall: FirFunctionCall, data: D): R  = visitElement(functionCall, data)

    private fun visitFunctionCallChildren(functionCall: FirFunctionCall, data: D) {
        functionCall.typeRef.accept(this, data)
        functionCall.annotations.forEach { it.accept(this, data) }
        functionCall.contextReceiverArguments.forEach { it.accept(this, data) }
        functionCall.typeArguments.forEach { it.accept(this, data) }
        functionCall.explicitReceiver?.accept(this, data)
        functionCall.argumentList.accept(this, data)
        functionCall.calleeReference.accept(this, data)
    }

    open fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: D): R  = visitElement(integerLiteralOperatorCall, data)

    private fun visitIntegerLiteralOperatorCallChildren(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: D) {
        integerLiteralOperatorCall.typeRef.accept(this, data)
        integerLiteralOperatorCall.annotations.forEach { it.accept(this, data) }
        integerLiteralOperatorCall.contextReceiverArguments.forEach { it.accept(this, data) }
        integerLiteralOperatorCall.typeArguments.forEach { it.accept(this, data) }
        integerLiteralOperatorCall.explicitReceiver?.accept(this, data)
        integerLiteralOperatorCall.argumentList.accept(this, data)
        integerLiteralOperatorCall.calleeReference.accept(this, data)
    }

    open fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: D): R  = visitElement(implicitInvokeCall, data)

    private fun visitImplicitInvokeCallChildren(implicitInvokeCall: FirImplicitInvokeCall, data: D) {
        implicitInvokeCall.typeRef.accept(this, data)
        implicitInvokeCall.annotations.forEach { it.accept(this, data) }
        implicitInvokeCall.contextReceiverArguments.forEach { it.accept(this, data) }
        implicitInvokeCall.typeArguments.forEach { it.accept(this, data) }
        implicitInvokeCall.explicitReceiver?.accept(this, data)
        implicitInvokeCall.argumentList.accept(this, data)
        implicitInvokeCall.calleeReference.accept(this, data)
    }

    open fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: D): R  = visitElement(delegatedConstructorCall, data)

    private fun visitDelegatedConstructorCallChildren(delegatedConstructorCall: FirDelegatedConstructorCall, data: D) {
        delegatedConstructorCall.annotations.forEach { it.accept(this, data) }
        delegatedConstructorCall.argumentList.accept(this, data)
        delegatedConstructorCall.contextReceiverArguments.forEach { it.accept(this, data) }
        delegatedConstructorCall.constructedTypeRef.accept(this, data)
        delegatedConstructorCall.calleeReference.accept(this, data)
    }

    open fun visitComponentCall(componentCall: FirComponentCall, data: D): R  = visitElement(componentCall, data)

    private fun visitComponentCallChildren(componentCall: FirComponentCall, data: D) {
        componentCall.typeRef.accept(this, data)
        componentCall.annotations.forEach { it.accept(this, data) }
        componentCall.contextReceiverArguments.forEach { it.accept(this, data) }
        componentCall.typeArguments.forEach { it.accept(this, data) }
        componentCall.argumentList.accept(this, data)
        componentCall.calleeReference.accept(this, data)
        componentCall.explicitReceiver.accept(this, data)
    }

    open fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: D): R  = visitElement(callableReferenceAccess, data)

    private fun visitCallableReferenceAccessChildren(callableReferenceAccess: FirCallableReferenceAccess, data: D) {
        callableReferenceAccess.typeRef.accept(this, data)
        callableReferenceAccess.annotations.forEach { it.accept(this, data) }
        callableReferenceAccess.contextReceiverArguments.forEach { it.accept(this, data) }
        callableReferenceAccess.typeArguments.forEach { it.accept(this, data) }
        callableReferenceAccess.explicitReceiver?.accept(this, data)
        callableReferenceAccess.calleeReference.accept(this, data)
    }

    open fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: D): R  = visitElement(thisReceiverExpression, data)

    private fun visitThisReceiverExpressionChildren(thisReceiverExpression: FirThisReceiverExpression, data: D) {
        thisReceiverExpression.typeRef.accept(this, data)
        thisReceiverExpression.annotations.forEach { it.accept(this, data) }
        thisReceiverExpression.contextReceiverArguments.forEach { it.accept(this, data) }
        thisReceiverExpression.typeArguments.forEach { it.accept(this, data) }
        thisReceiverExpression.explicitReceiver?.accept(this, data)
        thisReceiverExpression.calleeReference.accept(this, data)
    }

    open fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: D): R  = visitElement(safeCallExpression, data)

    private fun visitSafeCallExpressionChildren(safeCallExpression: FirSafeCallExpression, data: D) {
        safeCallExpression.typeRef.accept(this, data)
        safeCallExpression.annotations.forEach { it.accept(this, data) }
        safeCallExpression.receiver.accept(this, data)
        safeCallExpression.selector.accept(this, data)
    }

    open fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: D): R  = visitElement(checkedSafeCallSubject, data)

    private fun visitCheckedSafeCallSubjectChildren(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: D) {
        checkedSafeCallSubject.typeRef.accept(this, data)
        checkedSafeCallSubject.annotations.forEach { it.accept(this, data) }
    }

    open fun visitGetClassCall(getClassCall: FirGetClassCall, data: D): R  = visitElement(getClassCall, data)

    private fun visitGetClassCallChildren(getClassCall: FirGetClassCall, data: D) {
        getClassCall.typeRef.accept(this, data)
        getClassCall.annotations.forEach { it.accept(this, data) }
        getClassCall.argumentList.accept(this, data)
    }

    open fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: D): R  = visitElement(wrappedExpression, data)

    private fun visitWrappedExpressionChildren(wrappedExpression: FirWrappedExpression, data: D) {
        wrappedExpression.typeRef.accept(this, data)
        wrappedExpression.annotations.forEach { it.accept(this, data) }
        wrappedExpression.expression.accept(this, data)
    }

    open fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D): R  = visitElement(wrappedArgumentExpression, data)

    private fun visitWrappedArgumentExpressionChildren(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D) {
        wrappedArgumentExpression.typeRef.accept(this, data)
        wrappedArgumentExpression.annotations.forEach { it.accept(this, data) }
        wrappedArgumentExpression.expression.accept(this, data)
    }

    open fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): R  = visitElement(lambdaArgumentExpression, data)

    private fun visitLambdaArgumentExpressionChildren(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D) {
        lambdaArgumentExpression.typeRef.accept(this, data)
        lambdaArgumentExpression.annotations.forEach { it.accept(this, data) }
        lambdaArgumentExpression.expression.accept(this, data)
    }

    open fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): R  = visitElement(spreadArgumentExpression, data)

    private fun visitSpreadArgumentExpressionChildren(spreadArgumentExpression: FirSpreadArgumentExpression, data: D) {
        spreadArgumentExpression.typeRef.accept(this, data)
        spreadArgumentExpression.annotations.forEach { it.accept(this, data) }
        spreadArgumentExpression.expression.accept(this, data)
    }

    open fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): R  = visitElement(namedArgumentExpression, data)

    private fun visitNamedArgumentExpressionChildren(namedArgumentExpression: FirNamedArgumentExpression, data: D) {
        namedArgumentExpression.typeRef.accept(this, data)
        namedArgumentExpression.annotations.forEach { it.accept(this, data) }
        namedArgumentExpression.expression.accept(this, data)
    }

    open fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: D): R  = visitElement(varargArgumentsExpression, data)

    private fun visitVarargArgumentsExpressionChildren(varargArgumentsExpression: FirVarargArgumentsExpression, data: D) {
        varargArgumentsExpression.typeRef.accept(this, data)
        varargArgumentsExpression.annotations.forEach { it.accept(this, data) }
        varargArgumentsExpression.arguments.forEach { it.accept(this, data) }
        varargArgumentsExpression.varargElementType.accept(this, data)
    }

    open fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: D): R  = visitElement(resolvedQualifier, data)

    private fun visitResolvedQualifierChildren(resolvedQualifier: FirResolvedQualifier, data: D) {
        resolvedQualifier.typeRef.accept(this, data)
        resolvedQualifier.annotations.forEach { it.accept(this, data) }
        resolvedQualifier.typeArguments.forEach { it.accept(this, data) }
    }

    open fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: D): R  = visitElement(errorResolvedQualifier, data)

    private fun visitErrorResolvedQualifierChildren(errorResolvedQualifier: FirErrorResolvedQualifier, data: D) {
        errorResolvedQualifier.typeRef.accept(this, data)
        errorResolvedQualifier.annotations.forEach { it.accept(this, data) }
        errorResolvedQualifier.typeArguments.forEach { it.accept(this, data) }
    }

    open fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D): R  = visitElement(resolvedReifiedParameterReference, data)

    private fun visitResolvedReifiedParameterReferenceChildren(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D) {
        resolvedReifiedParameterReference.typeRef.accept(this, data)
        resolvedReifiedParameterReference.annotations.forEach { it.accept(this, data) }
    }

    open fun visitReturnExpression(returnExpression: FirReturnExpression, data: D): R  = visitElement(returnExpression, data)

    private fun visitReturnExpressionChildren(returnExpression: FirReturnExpression, data: D) {
        returnExpression.typeRef.accept(this, data)
        returnExpression.annotations.forEach { it.accept(this, data) }
        returnExpression.result.accept(this, data)
    }

    open fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: D): R  = visitElement(stringConcatenationCall, data)

    private fun visitStringConcatenationCallChildren(stringConcatenationCall: FirStringConcatenationCall, data: D) {
        stringConcatenationCall.annotations.forEach { it.accept(this, data) }
        stringConcatenationCall.argumentList.accept(this, data)
        stringConcatenationCall.typeRef.accept(this, data)
    }

    open fun visitThrowExpression(throwExpression: FirThrowExpression, data: D): R  = visitElement(throwExpression, data)

    private fun visitThrowExpressionChildren(throwExpression: FirThrowExpression, data: D) {
        throwExpression.typeRef.accept(this, data)
        throwExpression.annotations.forEach { it.accept(this, data) }
        throwExpression.exception.accept(this, data)
    }

    open fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: D): R  = visitElement(variableAssignment, data)

    private fun visitVariableAssignmentChildren(variableAssignment: FirVariableAssignment, data: D) {
        variableAssignment.calleeReference.accept(this, data)
        variableAssignment.annotations.forEach { it.accept(this, data) }
        variableAssignment.contextReceiverArguments.forEach { it.accept(this, data) }
        variableAssignment.typeArguments.forEach { it.accept(this, data) }
        variableAssignment.explicitReceiver?.accept(this, data)
        variableAssignment.lValueTypeRef.accept(this, data)
        variableAssignment.rValue.accept(this, data)
    }

    open fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: D): R  = visitElement(whenSubjectExpression, data)

    private fun visitWhenSubjectExpressionChildren(whenSubjectExpression: FirWhenSubjectExpression, data: D) {
        whenSubjectExpression.typeRef.accept(this, data)
        whenSubjectExpression.annotations.forEach { it.accept(this, data) }
    }

    open fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): R  = visitElement(wrappedDelegateExpression, data)

    private fun visitWrappedDelegateExpressionChildren(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D) {
        wrappedDelegateExpression.typeRef.accept(this, data)
        wrappedDelegateExpression.annotations.forEach { it.accept(this, data) }
        wrappedDelegateExpression.expression.accept(this, data)
        wrappedDelegateExpression.delegateProvider.accept(this, data)
    }

    open fun visitNamedReference(namedReference: FirNamedReference, data: D): R  = visitElement(namedReference, data)

    private fun visitNamedReferenceChildren(namedReference: FirNamedReference, data: D) {
    }

    open fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): R  = visitElement(errorNamedReference, data)

    private fun visitErrorNamedReferenceChildren(errorNamedReference: FirErrorNamedReference, data: D) {
    }

    open fun visitSuperReference(superReference: FirSuperReference, data: D): R  = visitElement(superReference, data)

    private fun visitSuperReferenceChildren(superReference: FirSuperReference, data: D) {
        superReference.superTypeRef.accept(this, data)
    }

    open fun visitThisReference(thisReference: FirThisReference, data: D): R  = visitElement(thisReference, data)

    private fun visitThisReferenceChildren(thisReference: FirThisReference, data: D) {
    }

    open fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: D): R  = visitElement(controlFlowGraphReference, data)

    private fun visitControlFlowGraphReferenceChildren(controlFlowGraphReference: FirControlFlowGraphReference, data: D) {
    }

    open fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: D): R  = visitElement(resolvedNamedReference, data)

    private fun visitResolvedNamedReferenceChildren(resolvedNamedReference: FirResolvedNamedReference, data: D) {
    }

    open fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: D): R  = visitElement(delegateFieldReference, data)

    private fun visitDelegateFieldReferenceChildren(delegateFieldReference: FirDelegateFieldReference, data: D) {
    }

    open fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: D): R  = visitElement(backingFieldReference, data)

    private fun visitBackingFieldReferenceChildren(backingFieldReference: FirBackingFieldReference, data: D) {
    }

    open fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: D): R  = visitElement(resolvedCallableReference, data)

    private fun visitResolvedCallableReferenceChildren(resolvedCallableReference: FirResolvedCallableReference, data: D) {
    }

    open fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): R  = visitElement(resolvedTypeRef, data)

    private fun visitResolvedTypeRefChildren(resolvedTypeRef: FirResolvedTypeRef, data: D) {
        resolvedTypeRef.annotations.forEach { it.accept(this, data) }
        resolvedTypeRef.delegatedTypeRef?.accept(this, data)
    }

    open fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): R  = visitElement(errorTypeRef, data)

    private fun visitErrorTypeRefChildren(errorTypeRef: FirErrorTypeRef, data: D) {
        errorTypeRef.annotations.forEach { it.accept(this, data) }
        errorTypeRef.delegatedTypeRef?.accept(this, data)
    }

    open fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): R  = visitElement(typeRefWithNullability, data)

    private fun visitTypeRefWithNullabilityChildren(typeRefWithNullability: FirTypeRefWithNullability, data: D) {
        typeRefWithNullability.annotations.forEach { it.accept(this, data) }
    }

    open fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: D): R  = visitElement(userTypeRef, data)

    private fun visitUserTypeRefChildren(userTypeRef: FirUserTypeRef, data: D) {
        userTypeRef.annotations.forEach { it.accept(this, data) }
    }

    open fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): R  = visitElement(dynamicTypeRef, data)

    private fun visitDynamicTypeRefChildren(dynamicTypeRef: FirDynamicTypeRef, data: D) {
        dynamicTypeRef.annotations.forEach { it.accept(this, data) }
    }

    open fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): R  = visitElement(functionTypeRef, data)

    private fun visitFunctionTypeRefChildren(functionTypeRef: FirFunctionTypeRef, data: D) {
        functionTypeRef.annotations.forEach { it.accept(this, data) }
        functionTypeRef.receiverTypeRef?.accept(this, data)
        functionTypeRef.valueParameters.forEach { it.accept(this, data) }
        functionTypeRef.returnTypeRef.accept(this, data)
        functionTypeRef.contextReceiverTypeRefs.forEach { it.accept(this, data) }
    }

    open fun visitIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: D): R  = visitElement(intersectionTypeRef, data)

    private fun visitIntersectionTypeRefChildren(intersectionTypeRef: FirIntersectionTypeRef, data: D) {
        intersectionTypeRef.annotations.forEach { it.accept(this, data) }
        intersectionTypeRef.leftType.accept(this, data)
        intersectionTypeRef.rightType.accept(this, data)
    }

    open fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): R  = visitElement(implicitTypeRef, data)

    private fun visitImplicitTypeRefChildren(implicitTypeRef: FirImplicitTypeRef, data: D) {
        implicitTypeRef.annotations.forEach { it.accept(this, data) }
    }

    open fun visitSmartCastedTypeRef(smartCastedTypeRef: FirSmartCastedTypeRef, data: D): R  = visitElement(smartCastedTypeRef, data)

    private fun visitSmartCastedTypeRefChildren(smartCastedTypeRef: FirSmartCastedTypeRef, data: D) {
        smartCastedTypeRef.annotations.forEach { it.accept(this, data) }
        smartCastedTypeRef.delegatedTypeRef?.accept(this, data)
    }

    open fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration, data: D): R  = visitElement(effectDeclaration, data)

    private fun visitEffectDeclarationChildren(effectDeclaration: FirEffectDeclaration, data: D) {
    }

    open fun visitContractDescription(contractDescription: FirContractDescription, data: D): R  = visitElement(contractDescription, data)

    private fun visitContractDescriptionChildren(contractDescription: FirContractDescription, data: D) {
    }

    open fun visitLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription, data: D): R  = visitElement(legacyRawContractDescription, data)

    private fun visitLegacyRawContractDescriptionChildren(legacyRawContractDescription: FirLegacyRawContractDescription, data: D) {
        legacyRawContractDescription.contractCall.accept(this, data)
    }

    open fun visitRawContractDescription(rawContractDescription: FirRawContractDescription, data: D): R  = visitElement(rawContractDescription, data)

    private fun visitRawContractDescriptionChildren(rawContractDescription: FirRawContractDescription, data: D) {
        rawContractDescription.rawEffects.forEach { it.accept(this, data) }
    }

    open fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription, data: D): R  = visitElement(resolvedContractDescription, data)

    private fun visitResolvedContractDescriptionChildren(resolvedContractDescription: FirResolvedContractDescription, data: D) {
        resolvedContractDescription.effects.forEach { it.accept(this, data) }
        resolvedContractDescription.unresolvedEffects.forEach { it.accept(this, data) }
    }

}
