// Generated from E:/!PROJECTS/IntelliJ IDEA/kotlin/compiler/fir/antlr2fir/src/org/jetbrains/kotlin/fir/antlr2fir/antlr4\KotlinParser.g4 by ANTLR 4.7
package org.jetbrains.kotlin.fir.antlr2fir.antlr4.generated;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link KotlinParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface KotlinParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link KotlinParser#kotlinFile}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKotlinFile(KotlinParser.KotlinFileContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#script}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScript(KotlinParser.ScriptContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#fileAnnotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileAnnotation(KotlinParser.FileAnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#packageHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackageHeader(KotlinParser.PackageHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#importList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportList(KotlinParser.ImportListContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#importHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportHeader(KotlinParser.ImportHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#importAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportAlias(KotlinParser.ImportAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#topLevelObject}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTopLevelObject(KotlinParser.TopLevelObjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#classDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDeclaration(KotlinParser.ClassDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#primaryConstructor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryConstructor(KotlinParser.PrimaryConstructorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#classParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassParameters(KotlinParser.ClassParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#classParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassParameter(KotlinParser.ClassParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#delegationSpecifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelegationSpecifiers(KotlinParser.DelegationSpecifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#annotatedDelegationSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotatedDelegationSpecifier(KotlinParser.AnnotatedDelegationSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#delegationSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelegationSpecifier(KotlinParser.DelegationSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#constructorInvocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorInvocation(KotlinParser.ConstructorInvocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#explicitDelegation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitDelegation(KotlinParser.ExplicitDelegationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#classBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBody(KotlinParser.ClassBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#classMemberDeclarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassMemberDeclarations(KotlinParser.ClassMemberDeclarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#classMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassMemberDeclaration(KotlinParser.ClassMemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#anonymousInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymousInitializer(KotlinParser.AnonymousInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#secondaryConstructor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSecondaryConstructor(KotlinParser.SecondaryConstructorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#constructorDelegationCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorDelegationCall(KotlinParser.ConstructorDelegationCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#enumClassBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumClassBody(KotlinParser.EnumClassBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#enumEntries}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumEntries(KotlinParser.EnumEntriesContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#enumEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumEntry(KotlinParser.EnumEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#functionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclaration(KotlinParser.FunctionDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#functionValueParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionValueParameters(KotlinParser.FunctionValueParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#functionValueParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionValueParameter(KotlinParser.FunctionValueParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(KotlinParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#setterParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetterParameter(KotlinParser.SetterParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#functionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionBody(KotlinParser.FunctionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#objectDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectDeclaration(KotlinParser.ObjectDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#companionObject}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompanionObject(KotlinParser.CompanionObjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#propertyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyDeclaration(KotlinParser.PropertyDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#multiVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiVariableDeclaration(KotlinParser.MultiVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(KotlinParser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#propertyDelegate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyDelegate(KotlinParser.PropertyDelegateContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#getter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetter(KotlinParser.GetterContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#setter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetter(KotlinParser.SetterContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAlias(KotlinParser.TypeAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameters(KotlinParser.TypeParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(KotlinParser.TypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeParameterModifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameterModifiers(KotlinParser.TypeParameterModifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeParameterModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameterModifier(KotlinParser.TypeParameterModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(KotlinParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeModifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeModifiers(KotlinParser.TypeModifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeModifier(KotlinParser.TypeModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#parenthesizedType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedType(KotlinParser.ParenthesizedTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#nullableType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullableType(KotlinParser.NullableTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeReference(KotlinParser.TypeReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#functionType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionType(KotlinParser.FunctionTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#receiverType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceiverType(KotlinParser.ReceiverTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#userType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserType(KotlinParser.UserTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#parenthesizedUserType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedUserType(KotlinParser.ParenthesizedUserTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#simpleUserType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleUserType(KotlinParser.SimpleUserTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#functionTypeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTypeParameters(KotlinParser.FunctionTypeParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeConstraints}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeConstraints(KotlinParser.TypeConstraintsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeConstraint(KotlinParser.TypeConstraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(KotlinParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatements(KotlinParser.StatementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(KotlinParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration(KotlinParser.DeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(KotlinParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(KotlinParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#disjunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisjunction(KotlinParser.DisjunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#conjunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConjunction(KotlinParser.ConjunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#equality}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEquality(KotlinParser.EqualityContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#comparison}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison(KotlinParser.ComparisonContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#infixOperation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInfixOperation(KotlinParser.InfixOperationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#elvisExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElvisExpression(KotlinParser.ElvisExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#infixFunctionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInfixFunctionCall(KotlinParser.InfixFunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#rangeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeExpression(KotlinParser.RangeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(KotlinParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(KotlinParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#asExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsExpression(KotlinParser.AsExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#prefixUnaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixUnaryExpression(KotlinParser.PrefixUnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#unaryPrefix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryPrefix(KotlinParser.UnaryPrefixContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#postfixUnaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixUnaryExpression(KotlinParser.PostfixUnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#postfixUnarySuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixUnarySuffix(KotlinParser.PostfixUnarySuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#directlyAssignableExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectlyAssignableExpression(KotlinParser.DirectlyAssignableExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#assignableExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignableExpression(KotlinParser.AssignableExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#assignableSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignableSuffix(KotlinParser.AssignableSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#indexingSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexingSuffix(KotlinParser.IndexingSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#navigationSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNavigationSuffix(KotlinParser.NavigationSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#callSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallSuffix(KotlinParser.CallSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#annotatedLambda}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotatedLambda(KotlinParser.AnnotatedLambdaContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#valueArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueArguments(KotlinParser.ValueArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArguments(KotlinParser.TypeArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeProjection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeProjection(KotlinParser.TypeProjectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeProjectionModifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeProjectionModifiers(KotlinParser.TypeProjectionModifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeProjectionModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeProjectionModifier(KotlinParser.TypeProjectionModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#valueArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueArgument(KotlinParser.ValueArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpression(KotlinParser.PrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#parenthesizedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpression(KotlinParser.ParenthesizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#collectionLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollectionLiteral(KotlinParser.CollectionLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#literalConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralConstant(KotlinParser.LiteralConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#stringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(KotlinParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#lineStringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLineStringLiteral(KotlinParser.LineStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#multiLineStringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiLineStringLiteral(KotlinParser.MultiLineStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#lineStringContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLineStringContent(KotlinParser.LineStringContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#lineStringExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLineStringExpression(KotlinParser.LineStringExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#multiLineStringContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiLineStringContent(KotlinParser.MultiLineStringContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#multiLineStringExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiLineStringExpression(KotlinParser.MultiLineStringExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#lambdaLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaLiteral(KotlinParser.LambdaLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#lambdaParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaParameters(KotlinParser.LambdaParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#lambdaParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaParameter(KotlinParser.LambdaParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#anonymousFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymousFunction(KotlinParser.AnonymousFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#functionLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionLiteral(KotlinParser.FunctionLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#objectLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectLiteral(KotlinParser.ObjectLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#thisExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisExpression(KotlinParser.ThisExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#superExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperExpression(KotlinParser.SuperExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#controlStructureBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitControlStructureBody(KotlinParser.ControlStructureBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#ifExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfExpression(KotlinParser.IfExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#whenExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenExpression(KotlinParser.WhenExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#whenEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenEntry(KotlinParser.WhenEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#whenCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenCondition(KotlinParser.WhenConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#rangeTest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeTest(KotlinParser.RangeTestContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#typeTest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeTest(KotlinParser.TypeTestContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#tryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTryExpression(KotlinParser.TryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#catchBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchBlock(KotlinParser.CatchBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#finallyBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinallyBlock(KotlinParser.FinallyBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#loopStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoopStatement(KotlinParser.LoopStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#forStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(KotlinParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(KotlinParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#doWhileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoWhileStatement(KotlinParser.DoWhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#jumpExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJumpExpression(KotlinParser.JumpExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#callableReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallableReference(KotlinParser.CallableReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#assignmentAndOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentAndOperator(KotlinParser.AssignmentAndOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#equalityOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityOperator(KotlinParser.EqualityOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(KotlinParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#inOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInOperator(KotlinParser.InOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#isOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsOperator(KotlinParser.IsOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#additiveOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveOperator(KotlinParser.AdditiveOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#multiplicativeOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeOperator(KotlinParser.MultiplicativeOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#asOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsOperator(KotlinParser.AsOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#prefixUnaryOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixUnaryOperator(KotlinParser.PrefixUnaryOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#postfixUnaryOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixUnaryOperator(KotlinParser.PostfixUnaryOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#memberAccessOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberAccessOperator(KotlinParser.MemberAccessOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#modifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifiers(KotlinParser.ModifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifier(KotlinParser.ModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#classModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassModifier(KotlinParser.ClassModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#memberModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberModifier(KotlinParser.MemberModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#visibilityModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVisibilityModifier(KotlinParser.VisibilityModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#varianceModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarianceModifier(KotlinParser.VarianceModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#functionModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionModifier(KotlinParser.FunctionModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#propertyModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyModifier(KotlinParser.PropertyModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#inheritanceModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInheritanceModifier(KotlinParser.InheritanceModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#parameterModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterModifier(KotlinParser.ParameterModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#reificationModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReificationModifier(KotlinParser.ReificationModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#platformModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlatformModifier(KotlinParser.PlatformModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel(KotlinParser.LabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(KotlinParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#singleAnnotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleAnnotation(KotlinParser.SingleAnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#multiAnnotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiAnnotation(KotlinParser.MultiAnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#annotationUseSiteTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationUseSiteTarget(KotlinParser.AnnotationUseSiteTargetContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#unescapedAnnotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnescapedAnnotation(KotlinParser.UnescapedAnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#simpleIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleIdentifier(KotlinParser.SimpleIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(KotlinParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#shebangLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShebangLine(KotlinParser.ShebangLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#quest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuest(KotlinParser.QuestContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#elvis}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElvis(KotlinParser.ElvisContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#safeNav}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSafeNav(KotlinParser.SafeNavContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#excl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExcl(KotlinParser.ExclContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#semi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSemi(KotlinParser.SemiContext ctx);
	/**
	 * Visit a parse tree produced by {@link KotlinParser#semis}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSemis(KotlinParser.SemisContext ctx);
}