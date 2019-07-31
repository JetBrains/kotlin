/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.contracts.EffectSystem;
import org.jetbrains.kotlin.contracts.parsing.ContractParsingServices;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.incremental.components.LookupTracker;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker;
import org.jetbrains.kotlin.resolve.calls.checkers.RttiExpressionChecker;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver;
import org.jetbrains.kotlin.types.WrappedTypeFactory;
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker;

import javax.inject.Inject;

public class ExpressionTypingComponents {
    /*package*/ GlobalContext globalContext;
    /*package*/ ModuleDescriptor moduleDescriptor;
    /*package*/ ExpressionTypingServices expressionTypingServices;
    /*package*/ CallResolver callResolver;
    /*package*/ PlatformToKotlinClassMap platformToKotlinClassMap;
    /*package*/ ControlStructureTypingUtils controlStructureTypingUtils;
    /*package*/ ForLoopConventionsChecker forLoopConventionsChecker;
    /*package*/ FakeCallResolver fakeCallResolver;
    /*package*/ KotlinBuiltIns builtIns;
    /*package*/ LocalClassifierAnalyzer localClassifierAnalyzer;
    /*package*/ FunctionDescriptorResolver functionDescriptorResolver;
    /*package*/ CallExpressionResolver callExpressionResolver;
    /*package*/ DoubleColonExpressionResolver doubleColonExpressionResolver;
    /*package*/ DescriptorResolver descriptorResolver;
    /*package*/ TypeResolver typeResolver;
    /*package*/ AnnotationResolver annotationResolver;
    /*package*/ ValueParameterResolver valueParameterResolver;
    /*package*/ DestructuringDeclarationResolver destructuringDeclarationResolver;
    /*package*/ ConstantExpressionEvaluator constantExpressionEvaluator;
    /*package*/ ModifiersChecker modifiersChecker;
    /*package*/ DataFlowAnalyzer dataFlowAnalyzer;
    /*package*/ Iterable<CallChecker> callCheckers;
    /*package*/ IdentifierChecker identifierChecker;
    /*package*/ DeclarationsCheckerBuilder declarationsCheckerBuilder;
    /*package*/ LocalVariableResolver localVariableResolver;
    /*package*/ LookupTracker lookupTracker;
    /*package*/ OverloadChecker overloadChecker;
    /*package*/ LanguageVersionSettings languageVersionSettings;
    /*package*/ Iterable<RttiExpressionChecker> rttiExpressionCheckers;
    /*package*/ WrappedTypeFactory wrappedTypeFactory;
    /*package*/ CollectionLiteralResolver collectionLiteralResolver;
    /*package*/ DeprecationResolver deprecationResolver;
    /*package*/ EffectSystem effectSystem;
    /*package*/ ContractParsingServices contractParsingServices;
    /*package*/ DataFlowValueFactory dataFlowValueFactory;
    /*package*/ NewKotlinTypeChecker kotlinTypeChecker;


    @Inject
    public void setGlobalContext(@NotNull GlobalContext globalContext) {
        this.globalContext = globalContext;
    }

    @Inject
    public void setModuleDescriptor(@NotNull ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setCallResolver(@NotNull CallResolver callResolver) {
        this.callResolver = callResolver;
    }

    @Inject
    public void setPlatformToKotlinClassMap(@NotNull PlatformToKotlinClassMap platformToKotlinClassMap) {
        this.platformToKotlinClassMap = platformToKotlinClassMap;
    }

    @Inject
    public void setControlStructureTypingUtils(@NotNull ControlStructureTypingUtils controlStructureTypingUtils) {
        this.controlStructureTypingUtils = controlStructureTypingUtils;
    }

    @Inject
    public void setForLoopConventionsChecker(@NotNull ForLoopConventionsChecker forLoopConventionsChecker) {
        this.forLoopConventionsChecker = forLoopConventionsChecker;
    }

    @Inject
    public void setFakeCallResolver(@NotNull FakeCallResolver fakeCallResolver) {
        this.fakeCallResolver = fakeCallResolver;
    }

    @Inject
    public void setBuiltIns(@NotNull KotlinBuiltIns builtIns) {
        this.builtIns = builtIns;
    }

    @Inject
    public void setLocalClassifierAnalyzer(@NotNull LocalClassifierAnalyzer localClassifierAnalyzer) {
        this.localClassifierAnalyzer = localClassifierAnalyzer;
    }

    @Inject
    public void setFunctionDescriptorResolver(FunctionDescriptorResolver functionDescriptorResolver) {
        this.functionDescriptorResolver = functionDescriptorResolver;
    }

    @Inject
    public void setCallExpressionResolver(CallExpressionResolver callExpressionResolver) {
        this.callExpressionResolver = callExpressionResolver;
    }

    @Inject
    public void setDoubleColonExpressionResolver(DoubleColonExpressionResolver doubleColonExpressionResolver) {
        this.doubleColonExpressionResolver = doubleColonExpressionResolver;
    }

    @Inject
    public void setDescriptorResolver(DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setAnnotationResolver(AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setValueParameterResolver(ValueParameterResolver valueParameterResolver) {
        this.valueParameterResolver = valueParameterResolver;
    }

    @Inject
    public void setDestructuringDeclarationResolver(DestructuringDeclarationResolver destructuringDeclarationResolver) {
        this.destructuringDeclarationResolver = destructuringDeclarationResolver;
    }

    @Inject
    public void setConstantExpressionEvaluator(@NotNull ConstantExpressionEvaluator constantExpressionEvaluator) {
        this.constantExpressionEvaluator = constantExpressionEvaluator;
    }

    @Inject
    public void setModifiersChecker(@NotNull ModifiersChecker modifiersChecker) {
        this.modifiersChecker = modifiersChecker;
    }

    @Inject
    public void setIdentifierChecker(@NotNull IdentifierChecker identifierChecker) {
        this.identifierChecker = identifierChecker;
    }

    @Inject
    public void setDataFlowAnalyzer(@NotNull DataFlowAnalyzer dataFlowAnalyzer) {
        this.dataFlowAnalyzer = dataFlowAnalyzer;
    }

    @Inject
    public void setCallCheckers(@NotNull Iterable<CallChecker> callCheckers) {
        this.callCheckers = callCheckers;
    }

    @Inject
    public void setDeclarationsCheckerBuilder(@NotNull DeclarationsCheckerBuilder declarationsCheckerBuilder) {
        this.declarationsCheckerBuilder = declarationsCheckerBuilder;
    }

    @Inject
    public void setLocalVariableResolver(@NotNull LocalVariableResolver localVariableResolver) {
        this.localVariableResolver = localVariableResolver;
    }

    @Inject
    public void setLookupTracker(@NotNull LookupTracker lookupTracker) {
        this.lookupTracker = lookupTracker;
    }

    @Inject
    public void setOverloadChecker(OverloadChecker overloadChecker) {
        this.overloadChecker = overloadChecker;
    }

    @Inject
    public void setLanguageVersionSettings(@NotNull LanguageVersionSettings languageVersionSettings) {
        this.languageVersionSettings = languageVersionSettings;
    }

    @Inject
    public void setRttiExpressionCheckers(@NotNull Iterable<RttiExpressionChecker> rttiExpressionCheckers) {
        this.rttiExpressionCheckers = rttiExpressionCheckers;
    }

    @Inject
    public void setWrappedTypeFactory(WrappedTypeFactory wrappedTypeFactory) {
        this.wrappedTypeFactory = wrappedTypeFactory;
    }

    @Inject
    public void setCollectionLiteralResolver(CollectionLiteralResolver collectionLiteralResolver) {
        this.collectionLiteralResolver = collectionLiteralResolver;
    }

    @Inject
    public void setDeprecationResolver(DeprecationResolver deprecationResolver) {
        this.deprecationResolver = deprecationResolver;
    }

    @Inject
    public void setEffectSystem(@NotNull EffectSystem effectSystem) {
        this.effectSystem = effectSystem;
    }

    @Inject
    public void setContractParsingServices(@NotNull ContractParsingServices contractParsingServices) {
        this.contractParsingServices = contractParsingServices;
    }

    @Inject
    public void setDataFlowValueFactory(@NotNull DataFlowValueFactory dataFlowValueFactory) {
        this.dataFlowValueFactory = dataFlowValueFactory;
    }

    @Inject
    public void setKotlinTypeChecker(@NotNull NewKotlinTypeChecker kotlinTypeChecker) {
        this.kotlinTypeChecker = kotlinTypeChecker;
    }
}
