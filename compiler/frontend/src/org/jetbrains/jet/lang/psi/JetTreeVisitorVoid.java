package org.jetbrains.jet.lang.psi;

import java.util.List;

/**
 * @author svtk
 */
public class JetTreeVisitorVoid extends JetVisitorVoid {
    @Override
    public void visitNamespace(JetNamespace namespace) {
        List<JetImportDirective> importDirectives = namespace.getImportDirectives();
        for (JetImportDirective directive : importDirectives) {
            directive.accept(this);
        }
        List<JetDeclaration> declarations = namespace.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.accept(this);
        }
    }

    @Override
    public void visitClass(JetClass klass) {
        List<JetDeclaration> declarations = klass.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.accept(this);
        }
    }

    @Override
    public void visitClassObject(JetClassObject classObject) {
        JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
        if (objectDeclaration != null) {
            objectDeclaration.accept(this);
        }
    }

    @Override
    public void visitConstructor(JetConstructor constructor) {
        visitDeclarationWithBody(constructor);
    }

    @Override
    public void visitNamedFunction(JetNamedFunction function) {
        visitDeclarationWithBody(function);
    }

    @Override
    public void visitProperty(JetProperty property) {
        List<JetPropertyAccessor> accessors = property.getAccessors();
        for (JetPropertyAccessor accessor : accessors) {
            accessor.accept(this);
        }
        JetExpression initializer = property.getInitializer();
        if (initializer != null) {
            initializer.accept(this);
        }
    }

    @Override
    public void visitTypedef(JetTypedef typedef) {
        super.visitTypedef(typedef);
    }

    @Override
    public void visitJetFile(JetFile file) {
        JetNamespace rootNamespace = file.getRootNamespace();
        rootNamespace.accept(this);
    }

    @Override
    public void visitImportDirective(JetImportDirective importDirective) {
        super.visitImportDirective(importDirective);
    }

    @Override
    public void visitClassBody(JetClassBody classBody) {
        List<JetDeclaration> declarations = classBody.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.accept(this);
        }
        List<JetConstructor> secondaryConstructors = classBody.getSecondaryConstructors();
        for (JetConstructor constructor : secondaryConstructors) {
            constructor.accept(this);
        }
    }

    @Override
    public void visitNamespaceBody(JetNamespaceBody body) {
        List<JetDeclaration> declarations = body.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.accept(this);
        }
    }

    @Override
    public void visitModifierList(JetModifierList list) {
        super.visitModifierList(list);
    }

    @Override
    public void visitAnnotation(JetAnnotation annotation) {
        super.visitAnnotation(annotation);
    }

    @Override
    public void visitAnnotationEntry(JetAnnotationEntry annotationEntry) {
        super.visitAnnotationEntry(annotationEntry);
    }

    @Override
    public void visitTypeParameterList(JetTypeParameterList list) {
        List<JetTypeParameter> parameters = list.getParameters();
        for (JetTypeParameter parameter : parameters) {
            parameter.accept(this);
        }
    }

    @Override
    public void visitTypeParameter(JetTypeParameter parameter) {
        super.visitTypeParameter(parameter);
    }

    @Override
    public void visitEnumEntry(JetEnumEntry enumEntry) {
        List<JetDelegationSpecifier> delegationSpecifiers = enumEntry.getDelegationSpecifiers();
        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            delegationSpecifier.accept(this);
        }
        JetModifierList modifierList = enumEntry.getModifierList();
        if (modifierList != null) {
            modifierList.accept(this);
        }
    }

    @Override
    public void visitParameterList(JetParameterList list) {
        List<JetParameter> parameters = list.getParameters();
        for (JetParameter parameter : parameters) {
            parameter.accept(this);
        }
    }

    @Override
    public void visitParameter(JetParameter parameter) {
        super.visitParameter(parameter);
    }

    @Override
    public void visitDelegationSpecifierList(JetDelegationSpecifierList list) {
        List<JetDelegationSpecifier> delegationSpecifiers = list.getDelegationSpecifiers();
        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            delegationSpecifier.accept(this);
        }
    }

    @Override
    public void visitDelegationSpecifier(JetDelegationSpecifier specifier) {
        super.visitDelegationSpecifier(specifier);
    }

    @Override
    public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
        super.visitDelegationByExpressionSpecifier(specifier);
    }

    @Override
    public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
        super.visitDelegationToSuperCallSpecifier(call);
    }

    @Override
    public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
        super.visitDelegationToSuperClassSpecifier(specifier);
    }

    @Override
    public void visitDelegationToThisCall(JetDelegatorToThisCall thisCall) {
        super.visitDelegationToThisCall(thisCall);
    }

    @Override
    public void visitTypeReference(JetTypeReference typeReference) {
        super.visitTypeReference(typeReference);
    }

    @Override
    public void visitValueArgumentList(JetValueArgumentList list) {
        List<JetValueArgument> arguments = list.getArguments();
        for (JetValueArgument argument : arguments) {
            argument.accept(this);
        }
    }

    @Override
    public void visitArgument(JetValueArgument argument) {
        super.visitArgument(argument);
    }

    @Override
    public void visitLoopExpression(JetLoopExpression loopExpression) {
        JetExpression body = loopExpression.getBody();
        if (body != null) {
            body.accept(this);
        }
    }

    @Override
    public void visitConstantExpression(JetConstantExpression expression) {
        super.visitConstantExpression(expression);
    }

    @Override
    public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
        super.visitSimpleNameExpression(expression);
    }

    @Override
    public void visitReferenceExpression(JetReferenceExpression expression) {
        super.visitReferenceExpression(expression);
    }

    @Override
    public void visitTupleExpression(JetTupleExpression expression) {
        super.visitTupleExpression(expression);
    }

    @Override
    public void visitPrefixExpression(JetPrefixExpression expression) {
        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression != null) {
            baseExpression.accept(this);
        }
    }

    @Override
    public void visitPostfixExpression(JetPostfixExpression expression) {
        JetExpression baseExpression = expression.getBaseExpression();
        baseExpression.accept(this);
    }

    @Override
    public void visitUnaryExpression(JetUnaryExpression expression) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;
        baseExpression.accept(this);
    }

    @Override
    public void visitBinaryExpression(JetBinaryExpression expression) {
        JetExpression left = expression.getLeft();
        left.accept(this);
        JetExpression right = expression.getRight();
        if (right != null) {
            right.accept(this);
        }
        super.visitBinaryExpression(expression);
    }

    @Override
    public void visitReturnExpression(JetReturnExpression expression) {
        JetExpression returnedExpression = expression.getReturnedExpression();
        if (returnedExpression != null) {
            returnedExpression.accept(this);
        }
    }

    @Override
    public void visitLabelQualifiedExpression(JetLabelQualifiedExpression expression) {
        JetExpression labeledExpression = expression.getLabeledExpression();
        if (labeledExpression != null) {
            labeledExpression.accept(this);
        }
    }

    @Override
    public void visitThrowExpression(JetThrowExpression expression) {
        JetExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            thrownExpression.accept(this);
        }
    }

    @Override
    public void visitBreakExpression(JetBreakExpression expression) {
        super.visitBreakExpression(expression);
    }

    @Override
    public void visitContinueExpression(JetContinueExpression expression) {
        super.visitContinueExpression(expression);
    }

    @Override
    public void visitIfExpression(JetIfExpression expression) {
        JetExpression condition = expression.getCondition();
        if (condition != null) {
            condition.accept(this);
        }
        JetExpression then = expression.getThen();
        if (then != null) {
            then.accept(this);
        }
        JetExpression anElse = expression.getElse();
        if (anElse != null) {
            anElse.accept(this);
        }
    }

    @Override
    public void visitWhenExpression(JetWhenExpression expression) {
        List<JetWhenEntry> entries = expression.getEntries();
        for (JetWhenEntry entry : entries) {
            entry.accept(this);
        }
        JetExpression subjectExpression = expression.getSubjectExpression();
        if (subjectExpression != null) {
            subjectExpression.accept(this);
        }
    }

    @Override
    public void visitTryExpression(JetTryExpression expression) {
        JetBlockExpression tryBlock = expression.getTryBlock();
        tryBlock.accept(this);
        List<JetCatchClause> catchClauses = expression.getCatchClauses();
        for (JetCatchClause catchClause : catchClauses) {
            catchClause.accept(this);
        }
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        if (finallyBlock != null) {
            finallyBlock.accept(this);
        }
    }
    
    @Override
    public void visitForExpression(JetForExpression expression) {
        JetParameter loopParameter = expression.getLoopParameter();
        if (loopParameter != null) {
            loopParameter.accept(this);
        }
        JetExpression loopRange = expression.getLoopRange();
        if (loopRange != null) {
            loopRange.accept(this);
        }
        visitLoopExpression(expression);
    }

    @Override
    public void visitWhileExpression(JetWhileExpression expression) {
        JetExpression condition = expression.getCondition();
        if (condition != null) {
            condition.accept(this);
        }
        visitLoopExpression(expression);
    }

    @Override
    public void visitDoWhileExpression(JetDoWhileExpression expression) {
        JetExpression condition = expression.getCondition();
        if (condition != null) {
            condition.accept(this);
        }
        visitLoopExpression(expression);
    }

    @Override
    public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        functionLiteral.accept(this);
        visitDeclarationWithBody(expression);
    }

    @Override
    public void visitAnnotatedExpression(JetAnnotatedExpression expression) {
        JetExpression baseExpression = expression.getBaseExpression();
        baseExpression.accept(this);
    }

    @Override
    public void visitCallExpression(JetCallExpression expression) {
        JetExpression calleeExpression = expression.getCalleeExpression();
        if (calleeExpression != null) {
            calleeExpression.accept(this);
        }
    }

    @Override
    public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
        JetExpression arrayExpression = expression.getArrayExpression();
        arrayExpression.accept(this);
        List<JetExpression> indexExpressions = expression.getIndexExpressions();
        for (JetExpression indexExpression : indexExpressions) {
            indexExpression.accept(this);
        }
    }

    @Override
    public void visitQualifiedExpression(JetQualifiedExpression expression) {
        JetExpression receiver = expression.getReceiverExpression();
        receiver.accept(this);
        JetExpression selector = expression.getSelectorExpression();
        if (selector != null) {
            selector.accept(this);
        }
    }

    @Override
    public void visitHashQualifiedExpression(JetHashQualifiedExpression expression) {
        visitQualifiedExpression(expression);
    }

    @Override
    public void visitDotQualifiedExpression(JetDotQualifiedExpression expression) {
        visitQualifiedExpression(expression);
    }

    @Override
    public void visitPredicateExpression(JetPredicateExpression expression) {
        visitQualifiedExpression(expression);
    }

    @Override
    public void visitSafeQualifiedExpression(JetSafeQualifiedExpression expression) {
        visitQualifiedExpression(expression);
    }

    @Override
    public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
        JetObjectDeclaration objectDeclaration = expression.getObjectDeclaration();
        objectDeclaration.accept(this);
    }

    @Override
    public void visitRootNamespaceExpression(JetRootNamespaceExpression expression) {
        super.visitRootNamespaceExpression(expression);
    }

    @Override
    public void visitBlockExpression(JetBlockExpression expression) {
        List<JetElement> statements = expression.getStatements();
        for (JetElement statement : statements) {
            statement.accept(this);
        }
    }

    @Override
    public void visitCatchSection(JetCatchClause catchClause) {
        JetParameter catchParameter = catchClause.getCatchParameter();
        if (catchParameter != null) {
            catchParameter.accept(this);
        }
        JetExpression catchBody = catchClause.getCatchBody();
        if (catchBody != null) {
            catchBody.accept(this);
        }
    }

    @Override
    public void visitFinallySection(JetFinallySection finallySection) {
        JetBlockExpression finalExpression = finallySection.getFinalExpression();
        finalExpression.accept(this);
    }

    @Override
    public void visitTypeArgumentList(JetTypeArgumentList typeArgumentList) {
        List<JetTypeProjection> arguments = typeArgumentList.getArguments();
        for (JetTypeProjection argument : arguments) {
            argument.accept(this);
        }
    }

    @Override
    public void visitThisExpression(JetThisExpression expression) {
        JetReferenceExpression thisReference = expression.getThisReference();
        thisReference.accept(this);
        JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
        if (superTypeQualifier != null) {
            superTypeQualifier.accept(this);
        }
        visitLabelQualifiedExpression(expression);
    }

    @Override
    public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
        JetExpression innerExpression = expression.getExpression();
        if (innerExpression != null) {
            innerExpression.accept(this);
        }
    }

    @Override
    public void visitInitializerList(JetInitializerList list) {
        List<JetDelegationSpecifier> initializers = list.getInitializers();
        for (JetDelegationSpecifier initializer : initializers) {
            initializer.accept(this);
        }
    }

    @Override
    public void visitAnonymousInitializer(JetClassInitializer initializer) {
        JetExpression body = initializer.getBody();
        body.accept(this);
    }

    @Override
    public void visitPropertyAccessor(JetPropertyAccessor accessor) {
        visitDeclarationWithBody(accessor);
    }

    @Override
    public void visitTypeConstraintList(JetTypeConstraintList list) {
        List<JetTypeConstraint> constraints = list.getConstraints();
        for (JetTypeConstraint constraint : constraints) {
            constraint.accept(this);
        }
    }

    @Override
    public void visitTypeConstraint(JetTypeConstraint constraint) {
        JetSimpleNameExpression subjectTypeParameterName = constraint.getSubjectTypeParameterName();
        if (subjectTypeParameterName != null) {
            subjectTypeParameterName.accept(this);
        }
    }

    @Override
    public void visitUserType(JetUserType type) {
        super.visitUserType(type);
    }

    @Override
    public void visitTupleType(JetTupleType type) {
        super.visitTupleType(type);
    }

    @Override
    public void visitFunctionType(JetFunctionType type) {
        super.visitFunctionType(type);
    }

    @Override
    public void visitSelfType(JetSelfType type) {
        super.visitSelfType(type);
    }

    @Override
    public void visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression) {
        JetExpression left = expression.getLeft();
        left.accept(this);
        JetTypeReference right = expression.getRight();
        if (right != null) {
            right.accept(this);
        }
    }

    @Override
    public void visitStringTemplateExpression(JetStringTemplateExpression expression) {
        JetStringTemplateEntry[] entries = expression.getEntries();
        for (JetStringTemplateEntry entry : entries) {
            entry.accept(this);
        }
    }

    @Override
    public void visitNamedDeclaration(JetNamedDeclaration declaration) {
        super.visitNamedDeclaration(declaration);
    }

    @Override
    public void visitNullableType(JetNullableType nullableType) {
        super.visitNullableType(nullableType);
    }

    @Override
    public void visitTypeProjection(JetTypeProjection typeProjection) {
        super.visitTypeProjection(typeProjection);
    }

    @Override
    public void visitWhenEntry(JetWhenEntry jetWhenEntry) {
        JetExpression expression = jetWhenEntry.getExpression();
        if (expression != null) {
            expression.accept(this);
        }
        JetWhenCondition[] conditions = jetWhenEntry.getConditions();
        for (JetWhenCondition condition : conditions) {
            condition.accept(this);
        }
    }

    @Override
    public void visitIsExpression(JetIsExpression expression) {
        JetExpression leftHandSide = expression.getLeftHandSide();
        leftHandSide.accept(this);
        JetSimpleNameExpression operationReference = expression.getOperationReference();
        operationReference.accept(this);
        JetPattern pattern = expression.getPattern();
        if (pattern != null) {
            pattern.accept(this);
        }
    }

    @Override
    public void visitWhenConditionCall(JetWhenConditionCall condition) {
        JetExpression callSuffixExpression = condition.getCallSuffixExpression();
        if (callSuffixExpression != null) {
            callSuffixExpression.accept(this);
        }
    }

    @Override
    public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
        JetPattern pattern = condition.getPattern();
        if (pattern != null) {
            pattern.accept(this);
        }
    }

    @Override
    public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
        JetSimpleNameExpression operationReference = condition.getOperationReference();
        operationReference.accept(this);
        JetExpression rangeExpression = condition.getRangeExpression();
        if (rangeExpression != null) {
            rangeExpression.accept(this);
        }
    }

    @Override
    public void visitTypePattern(JetTypePattern pattern) {
        super.visitTypePattern(pattern);
    }

    @Override
    public void visitWildcardPattern(JetWildcardPattern pattern) {
        super.visitWildcardPattern(pattern);
    }

    @Override
    public void visitExpressionPattern(JetExpressionPattern pattern) {
        JetExpression expression = pattern.getExpression();
        expression.accept(this);
    }

    @Override
    public void visitTuplePattern(JetTuplePattern pattern) {
        List<JetTuplePatternEntry> entries = pattern.getEntries();
        for (JetTuplePatternEntry entry : entries) {
            entry.accept(this);
        }
    }

    @Override
    public void visitDecomposerPattern(JetDecomposerPattern pattern) {
        JetTuplePattern argumentList = pattern.getArgumentList();
        argumentList.accept(this);
        JetExpression decomposerExpression = pattern.getDecomposerExpression();
        if (decomposerExpression != null) {
            decomposerExpression.accept(this);
        }
    }

    @Override
    public void visitObjectDeclaration(JetObjectDeclaration objectDeclaration) {
        List<JetDeclaration> declarations = objectDeclaration.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.accept(this);
        }
        JetDelegationSpecifierList delegationSpecifierList = objectDeclaration.getDelegationSpecifierList();
        if (delegationSpecifierList != null) {
            delegationSpecifierList.accept(this);
        }
    }

    @Override
    public void visitBindingPattern(JetBindingPattern pattern) {
        JetWhenCondition condition = pattern.getCondition();
        if (condition != null) {
            condition.accept(this);
        }
        JetProperty variableDeclaration = pattern.getVariableDeclaration();
        variableDeclaration.accept(this);
    }

    @Override
    public void visitStringTemplateEntry(JetStringTemplateEntry entry) {
        JetExpression expression = entry.getExpression();
        if (expression != null) {
            expression.accept(this);
        }
    }

    @Override
    public void visitStringTemplateEntryWithExpression(JetStringTemplateEntryWithExpression entry) {
        visitStringTemplateEntry(entry);
    }

    @Override
    public void visitBlockStringTemplateEntry(JetBlockStringTemplateEntry entry) {
        visitStringTemplateEntry(entry);
    }

    @Override
    public void visitSimpleNameStringTemplateEntry(JetSimpleNameStringTemplateEntry entry) {
        visitStringTemplateEntry(entry);
    }

    @Override
    public void visitLiteralStringTemplateEntry(JetLiteralStringTemplateEntry entry) {
        visitStringTemplateEntry(entry);
    }

    @Override
    public void visitEscapeStringTemplateEntry(JetEscapeStringTemplateEntry entry) {
        visitStringTemplateEntry(entry);
    }
    
    private void visitDeclarationWithBody(JetDeclarationWithBody declaration) {
        JetExpression bodyExpression = declaration.getBodyExpression();
        if (bodyExpression != null) {
            bodyExpression.accept(this);
        }
        List<JetParameter> valueParameters = declaration.getValueParameters();
        for (JetParameter valueParameter : valueParameters) {
            valueParameter.accept(this);
        }
    }
}
