package org.jetbrains.jet.lang.psi;

import java.util.List;

/**
 * @author svtk
 */
public class JetTreeVisitor<D> extends JetVisitor<Void, D> {
    @Override
    public Void visitNamespace(JetNamespace namespace, D data) {
        List<JetImportDirective> importDirectives = namespace.getImportDirectives();
        for (JetImportDirective directive : importDirectives) {
            directive.visit(this, data);
        }
        List<JetDeclaration> declarations = namespace.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitClass(JetClass klass, D data) {
        List<JetDeclaration> declarations = klass.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitClassObject(JetClassObject classObject, D data) {
        JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
        if (objectDeclaration != null) {
            objectDeclaration.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitConstructor(JetConstructor constructor, D data) {
        visitDeclarationWithBody(constructor, data);
        return null;
    }

    @Override
    public Void visitNamedFunction(JetNamedFunction function, D data) {
        visitDeclarationWithBody(function, data);
        return null;
    }

    @Override
    public Void visitProperty(JetProperty property, D data) {
        List<JetPropertyAccessor> accessors = property.getAccessors();
        for (JetPropertyAccessor accessor : accessors) {
            accessor.visit(this, data);
        }
        JetExpression initializer = property.getInitializer();
        if (initializer != null) {
            initializer.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitTypedef(JetTypedef typedef, D data) {
        return super.visitTypedef(typedef, data);
    }

    @Override
    public Void visitJetFile(JetFile file, D data) {
        JetNamespace rootNamespace = file.getRootNamespace();
        return rootNamespace.visit(this, data);
    }

    @Override
    public Void visitImportDirective(JetImportDirective importDirective, D data) {
        return super.visitImportDirective(importDirective, data);
    }

    @Override
    public Void visitClassBody(JetClassBody classBody, D data) {
        List<JetDeclaration> declarations = classBody.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.visit(this, data);
        }
        List<JetConstructor> secondaryConstructors = classBody.getSecondaryConstructors();
        for (JetConstructor constructor : secondaryConstructors) {
            constructor.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitNamespaceBody(JetNamespaceBody body, D data) {
        List<JetDeclaration> declarations = body.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitModifierList(JetModifierList list, D data) {
        return super.visitModifierList(list, data);
    }

    @Override
    public Void visitAnnotation(JetAnnotation annotation, D data) {
        return super.visitAnnotation(annotation, data);
    }

    @Override
    public Void visitAnnotationEntry(JetAnnotationEntry annotationEntry, D data) {
        return super.visitAnnotationEntry(annotationEntry, data);
    }

    @Override
    public Void visitTypeParameterList(JetTypeParameterList list, D data) {
        List<JetTypeParameter> parameters = list.getParameters();
        for (JetTypeParameter parameter : parameters) {
            parameter.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitTypeParameter(JetTypeParameter parameter, D data) {
        return super.visitTypeParameter(parameter, data);
    }

    @Override
    public Void visitEnumEntry(JetEnumEntry enumEntry, D data) {
        List<JetDelegationSpecifier> delegationSpecifiers = enumEntry.getDelegationSpecifiers();
        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            delegationSpecifier.visit(this, data);
        }
        JetModifierList modifierList = enumEntry.getModifierList();
        if (modifierList != null) {
            modifierList.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitParameterList(JetParameterList list, D data) {
        List<JetParameter> parameters = list.getParameters();
        for (JetParameter parameter : parameters) {
            parameter.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitParameter(JetParameter parameter, D data) {
        return super.visitParameter(parameter, data);
    }

    @Override
    public Void visitDelegationSpecifierList(JetDelegationSpecifierList list, D data) {
        List<JetDelegationSpecifier> delegationSpecifiers = list.getDelegationSpecifiers();
        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            delegationSpecifier.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitDelegationSpecifier(JetDelegationSpecifier specifier, D data) {
        return super.visitDelegationSpecifier(specifier, data);
    }

    @Override
    public Void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier, D data) {
        return super.visitDelegationByExpressionSpecifier(specifier, data);
    }

    @Override
    public Void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call, D data) {
        return super.visitDelegationToSuperCallSpecifier(call, data);
    }

    @Override
    public Void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier, D data) {
        return super.visitDelegationToSuperClassSpecifier(specifier, data);
    }

    @Override
    public Void visitDelegationToThisCall(JetDelegatorToThisCall thisCall, D data) {
        return super.visitDelegationToThisCall(thisCall, data);
    }

    @Override
    public Void visitTypeReference(JetTypeReference typeReference, D data) {
        return super.visitTypeReference(typeReference, data);
    }

    @Override
    public Void visitValueArgumentList(JetValueArgumentList list, D data) {
        List<JetValueArgument> arguments = list.getArguments();
        for (JetValueArgument argument : arguments) {
            argument.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitArgument(JetValueArgument argument, D data) {
        return super.visitArgument(argument, data);
    }

    @Override
    public Void visitLoopExpression(JetLoopExpression loopExpression, D data) {
        JetExpression body = loopExpression.getBody();
        if (body != null) {
            body.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitConstantExpression(JetConstantExpression expression, D data) {
        return super.visitConstantExpression(expression, data);
    }

    @Override
    public Void visitSimpleNameExpression(JetSimpleNameExpression expression, D data) {
        return super.visitSimpleNameExpression(expression, data);
    }

    @Override
    public Void visitReferenceExpression(JetReferenceExpression expression, D data) {
        return super.visitReferenceExpression(expression, data);
    }

    @Override
    public Void visitTupleExpression(JetTupleExpression expression, D data) {
        return super.visitTupleExpression(expression, data);
    }

    @Override
    public Void visitPrefixExpression(JetPrefixExpression expression, D data) {
        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression != null) {
            baseExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitPostfixExpression(JetPostfixExpression expression, D data) {
        JetExpression baseExpression = expression.getBaseExpression();
        baseExpression.visit(this, data);
        return null;
    }

    @Override
    public Void visitUnaryExpression(JetUnaryExpression expression, D data) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;
        baseExpression.visit(this, data);
        return null;
    }

    @Override
    public Void visitBinaryExpression(JetBinaryExpression expression, D data) {
        JetExpression left = expression.getLeft();
        left.visit(this, data);
        JetExpression right = expression.getRight();
        if (right != null) {
            right.visit(this, data);
        }
        return super.visitBinaryExpression(expression, data);
    }

    @Override
    public Void visitReturnExpression(JetReturnExpression expression, D data) {
        JetExpression returnedExpression = expression.getReturnedExpression();
        if (returnedExpression != null) {
            returnedExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitLabelQualifiedExpression(JetLabelQualifiedExpression expression, D data) {
        JetExpression labeledExpression = expression.getLabeledExpression();
        if (labeledExpression != null) {
            labeledExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitThrowExpression(JetThrowExpression expression, D data) {
        JetExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            thrownExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitBreakExpression(JetBreakExpression expression, D data) {
        return super.visitBreakExpression(expression, data);
    }

    @Override
    public Void visitContinueExpression(JetContinueExpression expression, D data) {
        return super.visitContinueExpression(expression, data);
    }

    @Override
    public Void visitIfExpression(JetIfExpression expression, D data) {
        JetExpression condition = expression.getCondition();
        if (condition != null) {
            condition.visit(this, data);
        }
        JetExpression then = expression.getThen();
        if (then != null) {
            then.visit(this, data);
        }
        JetExpression anElse = expression.getElse();
        if (anElse != null) {
            anElse.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitWhenExpression(JetWhenExpression expression, D data) {
        List<JetWhenEntry> entries = expression.getEntries();
        for (JetWhenEntry entry : entries) {
            entry.visit(this, data);
        }
        JetExpression subjectExpression = expression.getSubjectExpression();
        if (subjectExpression != null) {
            subjectExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitTryExpression(JetTryExpression expression, D data) {
        JetBlockExpression tryBlock = expression.getTryBlock();
        tryBlock.visit(this, data);
        List<JetCatchClause> catchClauses = expression.getCatchClauses();
        for (JetCatchClause catchClause : catchClauses) {
            catchClause.visit(this, data);
        }
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        if (finallyBlock != null) {
            finallyBlock.visit(this, data);
        }
        return null;
    }
    
    @Override
    public Void visitForExpression(JetForExpression expression, D data) {
        JetParameter loopParameter = expression.getLoopParameter();
        if (loopParameter != null) {
            loopParameter.visit(this, data);
        }
        JetExpression loopRange = expression.getLoopRange();
        if (loopRange != null) {
            loopRange.visit(this, data);
        }
        visitLoopExpression(expression, data);
        return null;
    }

    @Override
    public Void visitWhileExpression(JetWhileExpression expression, D data) {
        JetExpression condition = expression.getCondition();
        if (condition != null) {
            condition.visit(this, data);
        }
        visitLoopExpression(expression, data);
        return null;
    }

    @Override
    public Void visitDoWhileExpression(JetDoWhileExpression expression, D data) {
        JetExpression condition = expression.getCondition();
        if (condition != null) {
            condition.visit(this, data);
        }
        visitLoopExpression(expression, data);
        return null;
    }

    @Override
    public Void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, D data) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        functionLiteral.visit(this, data);
        visitDeclarationWithBody(expression, data);
        return null;
    }

    @Override
    public Void visitAnnotatedExpression(JetAnnotatedExpression expression, D data) {
        JetExpression baseExpression = expression.getBaseExpression();
        baseExpression.visit(this, data);
        return null;
    }

    @Override
    public Void visitCallExpression(JetCallExpression expression, D data) {
        JetExpression calleeExpression = expression.getCalleeExpression();
        if (calleeExpression != null) {
            calleeExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitArrayAccessExpression(JetArrayAccessExpression expression, D data) {
        JetExpression arrayExpression = expression.getArrayExpression();
        arrayExpression.visit(this, data);
        List<JetExpression> indexExpressions = expression.getIndexExpressions();
        for (JetExpression indexExpression : indexExpressions) {
            indexExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitQualifiedExpression(JetQualifiedExpression expression, D data) {
        JetExpression receiver = expression.getReceiverExpression();
        receiver.visit(this, data);
        JetExpression selector = expression.getSelectorExpression();
        if (selector != null) {
            selector.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitHashQualifiedExpression(JetHashQualifiedExpression expression, D data) {
        visitQualifiedExpression(expression, data);
        return null;
    }

    @Override
    public Void visitDotQualifiedExpression(JetDotQualifiedExpression expression, D data) {
        visitQualifiedExpression(expression, data);
        return null;
    }

    @Override
    public Void visitPredicateExpression(JetPredicateExpression expression, D data) {
        visitQualifiedExpression(expression, data);
        return null;
    }

    @Override
    public Void visitSafeQualifiedExpression(JetSafeQualifiedExpression expression, D data) {
        visitQualifiedExpression(expression, data);
        return null;
    }

    @Override
    public Void visitObjectLiteralExpression(JetObjectLiteralExpression expression, D data) {
        JetObjectDeclaration objectDeclaration = expression.getObjectDeclaration();
        objectDeclaration.visit(this, data);
        return null;
    }

    @Override
    public Void visitRootNamespaceExpression(JetRootNamespaceExpression expression, D data) {
        return super.visitRootNamespaceExpression(expression, data);
    }

    @Override
    public Void visitBlockExpression(JetBlockExpression expression, D data) {
        List<JetElement> statements = expression.getStatements();
        for (JetElement statement : statements) {
            statement.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitCatchSection(JetCatchClause catchClause, D data) {
        JetParameter catchParameter = catchClause.getCatchParameter();
        if (catchParameter != null) {
            catchParameter.visit(this, data);
        }
        JetExpression catchBody = catchClause.getCatchBody();
        if (catchBody != null) {
            catchBody.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitFinallySection(JetFinallySection finallySection, D data) {
        JetBlockExpression finalExpression = finallySection.getFinalExpression();
        finalExpression.visit(this, data);
        return null;
    }

    @Override
    public Void visitTypeArgumentList(JetTypeArgumentList typeArgumentList, D data) {
        List<JetTypeProjection> arguments = typeArgumentList.getArguments();
        for (JetTypeProjection argument : arguments) {
            argument.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitThisExpression(JetThisExpression expression, D data) {
        JetReferenceExpression thisReference = expression.getThisReference();
        thisReference.visit(this, data);
        JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
        if (superTypeQualifier != null) {
            superTypeQualifier.visit(this, data);
        }
        visitLabelQualifiedExpression(expression, data);
        return null;
    }

    @Override
    public Void visitParenthesizedExpression(JetParenthesizedExpression expression, D data) {
        JetExpression innerExpression = expression.getExpression();
        if (innerExpression != null) {
            innerExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitInitializerList(JetInitializerList list, D data) {
        List<JetDelegationSpecifier> initializers = list.getInitializers();
        for (JetDelegationSpecifier initializer : initializers) {
            initializer.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitAnonymousInitializer(JetClassInitializer initializer, D data) {
        JetExpression body = initializer.getBody();
        body.visit(this, data);
        return null;
    }

    @Override
    public Void visitPropertyAccessor(JetPropertyAccessor accessor, D data) {
        visitDeclarationWithBody(accessor, data);
        return null;
    }

    @Override
    public Void visitTypeConstraintList(JetTypeConstraintList list, D data) {
        List<JetTypeConstraint> constraints = list.getConstraints();
        for (JetTypeConstraint constraint : constraints) {
            constraint.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitTypeConstraint(JetTypeConstraint constraint, D data) {
        JetSimpleNameExpression subjectTypeParameterName = constraint.getSubjectTypeParameterName();
        if (subjectTypeParameterName != null) {
            subjectTypeParameterName.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitUserType(JetUserType type, D data) {
        return super.visitUserType(type, data);
    }

    @Override
    public Void visitTupleType(JetTupleType type, D data) {
        return super.visitTupleType(type, data);
    }

    @Override
    public Void visitFunctionType(JetFunctionType type, D data) {
        return super.visitFunctionType(type, data);
    }

    @Override
    public Void visitSelfType(JetSelfType type, D data) {
        return super.visitSelfType(type, data);
    }

    @Override
    public Void visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression, D data) {
        JetExpression left = expression.getLeft();
        left.visit(this, data);
        JetTypeReference right = expression.getRight();
        if (right != null) {
            right.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitStringTemplateExpression(JetStringTemplateExpression expression, D data) {
        JetStringTemplateEntry[] entries = expression.getEntries();
        for (JetStringTemplateEntry entry : entries) {
            entry.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitNamedDeclaration(JetNamedDeclaration declaration, D data) {
        return super.visitNamedDeclaration(declaration, data);
    }

    @Override
    public Void visitNullableType(JetNullableType nullableType, D data) {
        return super.visitNullableType(nullableType, data);
    }

    @Override
    public Void visitTypeProjection(JetTypeProjection typeProjection, D data) {
        return super.visitTypeProjection(typeProjection, data);
    }

    @Override
    public Void visitWhenEntry(JetWhenEntry jetWhenEntry, D data) {
        JetExpression expression = jetWhenEntry.getExpression();
        if (expression != null) {
            expression.visit(this, data);
        }
        JetWhenCondition[] conditions = jetWhenEntry.getConditions();
        for (JetWhenCondition condition : conditions) {
            condition.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitIsExpression(JetIsExpression expression, D data) {
        JetExpression leftHandSide = expression.getLeftHandSide();
        leftHandSide.visit(this, data);
        JetSimpleNameExpression operationReference = expression.getOperationReference();
        operationReference.visit(this, data);
        JetPattern pattern = expression.getPattern();
        if (pattern != null) {
            pattern.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitWhenConditionCall(JetWhenConditionCall condition, D data) {
        JetExpression callSuffixExpression = condition.getCallSuffixExpression();
        if (callSuffixExpression != null) {
            callSuffixExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition, D data) {
        JetPattern pattern = condition.getPattern();
        if (pattern != null) {
            pattern.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitWhenConditionInRange(JetWhenConditionInRange condition, D data) {
        JetSimpleNameExpression operationReference = condition.getOperationReference();
        operationReference.visit(this, data);
        JetExpression rangeExpression = condition.getRangeExpression();
        if (rangeExpression != null) {
            rangeExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitTypePattern(JetTypePattern pattern, D data) {
        return super.visitTypePattern(pattern, data);
    }

    @Override
    public Void visitWildcardPattern(JetWildcardPattern pattern, D data) {
        return super.visitWildcardPattern(pattern, data);
    }

    @Override
    public Void visitExpressionPattern(JetExpressionPattern pattern, D data) {
        JetExpression expression = pattern.getExpression();
        if (expression != null) {
            expression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitTuplePattern(JetTuplePattern pattern, D data) {
        List<JetTuplePatternEntry> entries = pattern.getEntries();
        for (JetTuplePatternEntry entry : entries) {
            entry.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitDecomposerPattern(JetDecomposerPattern pattern, D data) {
        JetTuplePattern argumentList = pattern.getArgumentList();
        argumentList.visit(this, data);
        JetExpression decomposerExpression = pattern.getDecomposerExpression();
        if (decomposerExpression != null) {
            decomposerExpression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitObjectDeclaration(JetObjectDeclaration objectDeclaration, D data) {
        List<JetDeclaration> declarations = objectDeclaration.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.visit(this, data);
        }
        JetDelegationSpecifierList delegationSpecifierList = objectDeclaration.getDelegationSpecifierList();
        if (delegationSpecifierList != null) {
            delegationSpecifierList.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitBindingPattern(JetBindingPattern pattern, D data) {
        JetWhenCondition condition = pattern.getCondition();
        if (condition != null) {
            condition.visit(this, data);
        }
        JetProperty variableDeclaration = pattern.getVariableDeclaration();
        variableDeclaration.visit(this, data);
        return null;
    }

    @Override
    public Void visitStringTemplateEntry(JetStringTemplateEntry entry, D data) {
        JetExpression expression = entry.getExpression();
        if (expression != null) {
            expression.visit(this, data);
        }
        return null;
    }

    @Override
    public Void visitStringTemplateEntryWithExpression(JetStringTemplateEntryWithExpression entry, D data) {
        visitStringTemplateEntry(entry, data);
        return null;
    }

    @Override
    public Void visitBlockStringTemplateEntry(JetBlockStringTemplateEntry entry, D data) {
        visitStringTemplateEntry(entry, data);
        return null;
    }

    @Override
    public Void visitSimpleNameStringTemplateEntry(JetSimpleNameStringTemplateEntry entry, D data) {
        visitStringTemplateEntry(entry, data);
        return null;
    }

    @Override
    public Void visitLiteralStringTemplateEntry(JetLiteralStringTemplateEntry entry, D data) {
        visitStringTemplateEntry(entry, data);
        return null;
    }

    @Override
    public Void visitEscapeStringTemplateEntry(JetEscapeStringTemplateEntry entry, D data) {
        visitStringTemplateEntry(entry, data);
        return null;
    }
    
    private Void visitDeclarationWithBody(JetDeclarationWithBody declaration, D data) {
        JetExpression bodyExpression = declaration.getBodyExpression();
        if (bodyExpression != null) {
            bodyExpression.visit(this, data);
        }
        List<JetParameter> valueParameters = declaration.getValueParameters();
        for (JetParameter valueParameter : valueParameters) {
            valueParameter.visit(this, data);
        }
        return null;
    }
}
