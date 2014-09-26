// This is a generated file. Not intended for manual editing.
package generated;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.openapi.diagnostic.Logger;
import static generated.KotlinTypes.*;
import static kt.KotlinParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import static org.jetbrains.jet.lexer.JetTokens.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class KotlinParser implements PsiParser {

  public static final Logger LOG_ = Logger.getInstance("generated.KotlinParser");

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, null);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    if (root_ == AND_OPERATION) {
      result_ = AND_OPERATION(builder_, 0);
    }
    else if (root_ == CHARACTER_CONSTANT) {
      result_ = CHARACTER_CONSTANT(builder_, 0);
    }
    else if (root_ == DOT_IDENTIFIER) {
      result_ = DOT_IDENTIFIER(builder_, 0);
    }
    else if (root_ == FLOAT_CONSTANT) {
      result_ = FLOAT_CONSTANT(builder_, 0);
    }
    else if (root_ == INTEGER_CONSTANT) {
      result_ = INTEGER_CONSTANT(builder_, 0);
    }
    else if (root_ == NULL) {
      result_ = NULL(builder_, 0);
    }
    else if (root_ == OR_OPERATION) {
      result_ = OR_OPERATION(builder_, 0);
    }
    else if (root_ == ADDITIVE_EXPRESSION_PLUS) {
      result_ = additiveExpressionPlus(builder_, 0);
    }
    else if (root_ == ADDITIVE_OPERATION) {
      result_ = additiveOperation(builder_, 0);
    }
    else if (root_ == ANNOTATED_EXPRESSION) {
      result_ = annotatedExpression(builder_, 0);
    }
    else if (root_ == ANNOTATION_ENTRY) {
      result_ = annotationEntry(builder_, 0);
    }
    else if (root_ == ANNOTATIONS) {
      result_ = annotations(builder_, 0);
    }
    else if (root_ == ANONYMOUS_INITIALIZER) {
      result_ = anonymousInitializer(builder_, 0);
    }
    else if (root_ == ARRAY_ACCESS) {
      result_ = arrayAccess(builder_, 0);
    }
    else if (root_ == ASSIGNMENT_EXPRESSION) {
      result_ = assignmentExpression(builder_, 0);
    }
    else if (root_ == ASSIGNMENT_OPERATOR) {
      result_ = assignmentOperator(builder_, 0);
    }
    else if (root_ == ASTERISK) {
      result_ = asterisk(builder_, 0);
    }
    else if (root_ == ATOMIC_PACKAGE) {
      result_ = atomicPackage(builder_, 0);
    }
    else if (root_ == BINARY_CONSTANT) {
      result_ = binaryConstant(builder_, 0);
    }
    else if (root_ == BLOCK) {
      result_ = block(builder_, 0);
    }
    else if (root_ == BODY) {
      result_ = body(builder_, 0);
    }
    else if (root_ == CALL_SUFFIX) {
      result_ = callSuffix(builder_, 0);
    }
    else if (root_ == CALLABLE_REFERENCE) {
      result_ = callableReference(builder_, 0);
    }
    else if (root_ == CATCH_BLOCK) {
      result_ = catchBlock(builder_, 0);
    }
    else if (root_ == CLASS_BODY) {
      result_ = classBody(builder_, 0);
    }
    else if (root_ == CLASS_DECLARATION) {
      result_ = classDeclaration(builder_, 0);
    }
    else if (root_ == CLASS_DECLARATION_LOCAL) {
      result_ = classDeclarationLocal(builder_, 0);
    }
    else if (root_ == CLASS_OBJECT) {
      result_ = classObject(builder_, 0);
    }
    else if (root_ == COMPARISON_EXPRESSION) {
      result_ = comparisonExpression(builder_, 0);
    }
    else if (root_ == COMPARISON_OPERATION) {
      result_ = comparisonOperation(builder_, 0);
    }
    else if (root_ == CONDITION) {
      result_ = condition(builder_, 0);
    }
    else if (root_ == CONJUNCTION_PLUS) {
      result_ = conjunctionPlus(builder_, 0);
    }
    else if (root_ == CONSTRUCTOR_CALLEE) {
      result_ = constructorCallee(builder_, 0);
    }
    else if (root_ == DELEGATION_SPECIFIER_EXT) {
      result_ = delegationSpecifierExt(builder_, 0);
    }
    else if (root_ == DELEGATOR_SUPER_CALL) {
      result_ = delegatorSuperCall(builder_, 0);
    }
    else if (root_ == DELEGATOR_SUPER_CLASS) {
      result_ = delegatorSuperClass(builder_, 0);
    }
    else if (root_ == DISJUNCTION_PLUS) {
      result_ = disjunctionPlus(builder_, 0);
    }
    else if (root_ == DO_WHILE_LOOP) {
      result_ = doWhileLoop(builder_, 0);
    }
    else if (root_ == DOT_QUALIFIED_EXPRESSION) {
      result_ = dotQualifiedExpression(builder_, 0);
    }
    else if (root_ == ELSE_EXPRESSION) {
      result_ = elseExpression(builder_, 0);
    }
    else if (root_ == ELVIS_ACCESS_EXPRESSION) {
      result_ = elvisAccessExpression(builder_, 0);
    }
    else if (root_ == ELVIS_OPERATION) {
      result_ = elvisOperation(builder_, 0);
    }
    else if (root_ == ENUM_CLASS_BODY) {
      result_ = enumClassBody(builder_, 0);
    }
    else if (root_ == ENUM_ENTRY) {
      result_ = enumEntry(builder_, 0);
    }
    else if (root_ == EQUALITY_COMPARISON_EXPRESSION) {
      result_ = equalityComparisonExpression(builder_, 0);
    }
    else if (root_ == EQUALITY_OPERATION) {
      result_ = equalityOperation(builder_, 0);
    }
    else if (root_ == EXPLICIT_DELEGATION) {
      result_ = explicitDelegation(builder_, 0);
    }
    else if (root_ == FIELD_IDENTIFIER_REFERENCE) {
      result_ = fieldIdentifierReference(builder_, 0);
    }
    else if (root_ == FINALLY_BLOCK) {
      result_ = finallyBlock(builder_, 0);
    }
    else if (root_ == FOR_LOOP) {
      result_ = forLoop(builder_, 0);
    }
    else if (root_ == FOR_VALUE_PARAMETER) {
      result_ = forValueParameter(builder_, 0);
    }
    else if (root_ == FUNCTION) {
      result_ = function(builder_, 0);
    }
    else if (root_ == FUNCTION_LITERAL) {
      result_ = functionLiteral(builder_, 0);
    }
    else if (root_ == FUNCTION_LITERAL_ARGUMENT) {
      result_ = functionLiteralArgument(builder_, 0);
    }
    else if (root_ == FUNCTION_LITERAL_EXPRESSION) {
      result_ = functionLiteralExpression(builder_, 0);
    }
    else if (root_ == FUNCTION_LITERAL_VALUE_PARAMETER_LIST_1) {
      result_ = functionLiteralValueParameterList1(builder_, 0);
    }
    else if (root_ == FUNCTION_LITERAL_VALUE_PARAMETER_LIST_2) {
      result_ = functionLiteralValueParameterList2(builder_, 0);
    }
    else if (root_ == FUNCTION_PARAMETER) {
      result_ = functionParameter(builder_, 0);
    }
    else if (root_ == FUNCTION_TYPE) {
      result_ = functionType(builder_, 0);
    }
    else if (root_ == FUNCTION_TYPE_RECEIVER) {
      result_ = functionTypeReceiver(builder_, 0);
    }
    else if (root_ == FUNCTION_TYPE_RECEIVER_REFERENCE) {
      result_ = functionTypeReceiverReference(builder_, 0);
    }
    else if (root_ == GETTER) {
      result_ = getter(builder_, 0);
    }
    else if (root_ == IF_EXPRESSION) {
      result_ = ifExpression(builder_, 0);
    }
    else if (root_ == IMPORT_DIRECTIVE) {
      result_ = importDirective(builder_, 0);
    }
    else if (root_ == IMPORT_LIST) {
      result_ = importList(builder_, 0);
    }
    else if (root_ == IN_OPERATION) {
      result_ = inOperation(builder_, 0);
    }
    else if (root_ == INDICES) {
      result_ = indices(builder_, 0);
    }
    else if (root_ == INFIX_FUNCTION_CALL_PLUS) {
      result_ = infixFunctionCallPlus(builder_, 0);
    }
    else if (root_ == INITIALIZER_EXT) {
      result_ = initializerExt(builder_, 0);
    }
    else if (root_ == IS_OPERATION) {
      result_ = isOperation(builder_, 0);
    }
    else if (root_ == JUMP_BREAK) {
      result_ = jumpBreak(builder_, 0);
    }
    else if (root_ == JUMP_CONTINUE) {
      result_ = jumpContinue(builder_, 0);
    }
    else if (root_ == JUMP_RETURN) {
      result_ = jumpReturn(builder_, 0);
    }
    else if (root_ == JUMP_THROW) {
      result_ = jumpThrow(builder_, 0);
    }
    else if (root_ == LABEL) {
      result_ = label(builder_, 0);
    }
    else if (root_ == LABEL_EXPRESSION) {
      result_ = labelExpression(builder_, 0);
    }
    else if (root_ == LABEL_LABEL) {
      result_ = labelLabel(builder_, 0);
    }
    else if (root_ == LEFT_FUNCTION_TYPE) {
      result_ = leftFunctionType(builder_, 0);
    }
    else if (root_ == LITERAL_STRING_TEMPLATE_ENTRY) {
      result_ = literalStringTemplateEntry(builder_, 0);
    }
    else if (root_ == LONG_ANNOTATION) {
      result_ = longAnnotation(builder_, 0);
    }
    else if (root_ == LONG_TEMPLATE) {
      result_ = longTemplate(builder_, 0);
    }
    else if (root_ == LOOP_RANGE) {
      result_ = loopRange(builder_, 0);
    }
    else if (root_ == MODIFIER_LIST) {
      result_ = modifierList(builder_, 0);
    }
    else if (root_ == MULTIPLE_VARIABLE_DECLARATIONS) {
      result_ = multipleVariableDeclarations(builder_, 0);
    }
    else if (root_ == MULTIPLICATIVE_EXPRESSION_PLUS) {
      result_ = multiplicativeExpressionPlus(builder_, 0);
    }
    else if (root_ == MULTIPLICATIVE_OPERATION) {
      result_ = multiplicativeOperation(builder_, 0);
    }
    else if (root_ == NAMED_INFIX_FIRST) {
      result_ = namedInfixFirst(builder_, 0);
    }
    else if (root_ == NAMED_INFIX_PLUS) {
      result_ = namedInfixPlus(builder_, 0);
    }
    else if (root_ == NULLABLE_TYPE) {
      result_ = nullableType(builder_, 0);
    }
    else if (root_ == OBJECT) {
      result_ = object(builder_, 0);
    }
    else if (root_ == OBJECT_DECLARATION) {
      result_ = objectDeclaration(builder_, 0);
    }
    else if (root_ == OBJECT_DECLARATION_NAME) {
      result_ = objectDeclarationName(builder_, 0);
    }
    else if (root_ == OBJECT_LITERAL) {
      result_ = objectLiteral(builder_, 0);
    }
    else if (root_ == OBJECT_NAME) {
      result_ = objectName(builder_, 0);
    }
    else if (root_ == OBJECT_UNNAMED) {
      result_ = objectUnnamed(builder_, 0);
    }
    else if (root_ == PACKAGE_DIRECTIVE) {
      result_ = packageDirective(builder_, 0);
    }
    else if (root_ == PACKAGE_HEADER) {
      result_ = packageHeader(builder_, 0);
    }
    else if (root_ == PARAMETER) {
      result_ = parameter(builder_, 0);
    }
    else if (root_ == PARAMETER_OR_MODIFIER_TYPE) {
      result_ = parameterOrModifierType(builder_, 0);
    }
    else if (root_ == PARENTHESIZED_EXPRESSION) {
      result_ = parenthesizedExpression(builder_, 0);
    }
    else if (root_ == PLUS_PLUS_AND_OTHERS_EXPRESSION) {
      result_ = plusPlusAndOthersExpression(builder_, 0);
    }
    else if (root_ == PLUS_PLUS_AND_OTHERS_OPERATION) {
      result_ = plusPlusAndOthersOperation(builder_, 0);
    }
    else if (root_ == PREFIX_OPERATION) {
      result_ = prefixOperation(builder_, 0);
    }
    else if (root_ == PREFIX_OPERATION_EXPRESSION) {
      result_ = prefixOperationExpression(builder_, 0);
    }
    else if (root_ == PRIMARY_CONSTRUCTOR_MODIFIER_LIST) {
      result_ = primaryConstructorModifierList(builder_, 0);
    }
    else if (root_ == PROPERTY) {
      result_ = property(builder_, 0);
    }
    else if (root_ == PROPERTY_DELEGATE) {
      result_ = propertyDelegate(builder_, 0);
    }
    else if (root_ == PROPERTY_LOCAL) {
      result_ = propertyLocal(builder_, 0);
    }
    else if (root_ == RANGE_EXPRESSION_PLUS) {
      result_ = rangeExpressionPlus(builder_, 0);
    }
    else if (root_ == RANGE_OPERATION) {
      result_ = rangeOperation(builder_, 0);
    }
    else if (root_ == REFERENCE_EXPRESSION) {
      result_ = referenceExpression(builder_, 0);
    }
    else if (root_ == SAFE_ACCESS_EXPRESSION) {
      result_ = safeAccessExpression(builder_, 0);
    }
    else if (root_ == SCRIPT) {
      result_ = script(builder_, 0);
    }
    else if (root_ == SELF_TYPE) {
      result_ = selfType(builder_, 0);
    }
    else if (root_ == SETTER) {
      result_ = setter(builder_, 0);
    }
    else if (root_ == SHORT_TEMPLATE_ENTRY) {
      result_ = shortTemplateEntry(builder_, 0);
    }
    else if (root_ == SIMPLE_NAME_OPERATION) {
      result_ = simpleNameOperation(builder_, 0);
    }
    else if (root_ == SIMPLE_USER_TYPE) {
      result_ = simpleUserType(builder_, 0);
    }
    else if (root_ == SIMPLE_USER_TYPE_ADD) {
      result_ = simpleUserTypeAdd(builder_, 0);
    }
    else if (root_ == SINGLE_VALUE_PARAMETER_LIST) {
      result_ = singleValueParameterList(builder_, 0);
    }
    else if (root_ == SINGLE_VALUE_PARAMETER_LIST_WITH_BRACKETS) {
      result_ = singleValueParameterListWithBrackets(builder_, 0);
    }
    else if (root_ == STATEMENTS_BLOCK) {
      result_ = statementsBlock(builder_, 0);
    }
    else if (root_ == STRING_TEMPLATE) {
      result_ = stringTemplate(builder_, 0);
    }
    else if (root_ == SUPER_EXPRESSION) {
      result_ = superExpression(builder_, 0);
    }
    else if (root_ == SUPER_REFERENCE) {
      result_ = superReference(builder_, 0);
    }
    else if (root_ == THEN_EXPRESSION) {
      result_ = thenExpression(builder_, 0);
    }
    else if (root_ == THEN_EXPRESSION_WITH_SEMI) {
      result_ = thenExpressionWithSemi(builder_, 0);
    }
    else if (root_ == THIS_EXPRESSION) {
      result_ = thisExpression(builder_, 0);
    }
    else if (root_ == THIS_REFERENCE) {
      result_ = thisReference(builder_, 0);
    }
    else if (root_ == TRY_BLOCK) {
      result_ = tryBlock(builder_, 0);
    }
    else if (root_ == TYPE) {
      result_ = type(builder_, 0);
    }
    else if (root_ == TYPE_ARGUMENT_LIST) {
      result_ = typeArgumentList(builder_, 0);
    }
    else if (root_ == TYPE_ARGUMENTS) {
      result_ = typeArguments(builder_, 0);
    }
    else if (root_ == TYPE_CONSTRAINT) {
      result_ = typeConstraint(builder_, 0);
    }
    else if (root_ == TYPE_CONSTRAINT_EXT) {
      result_ = typeConstraintExt(builder_, 0);
    }
    else if (root_ == TYPE_LIST) {
      result_ = typeList(builder_, 0);
    }
    else if (root_ == TYPE_OPERATION) {
      result_ = typeOperation(builder_, 0);
    }
    else if (root_ == TYPE_PARAMETER) {
      result_ = typeParameter(builder_, 0);
    }
    else if (root_ == TYPE_PARAMETERS) {
      result_ = typeParameters(builder_, 0);
    }
    else if (root_ == TYPE_PROJECTION) {
      result_ = typeProjection(builder_, 0);
    }
    else if (root_ == TYPE_RHS_PLUS) {
      result_ = typeRHSPlus(builder_, 0);
    }
    else if (root_ == TYPEDEF) {
      result_ = typedef(builder_, 0);
    }
    else if (root_ == USER_TYPE_REFERENCE) {
      result_ = userTypeReference(builder_, 0);
    }
    else if (root_ == VALUE_ARGUMENT) {
      result_ = valueArgument(builder_, 0);
    }
    else if (root_ == VALUE_ARGUMENT_NAME) {
      result_ = valueArgumentName(builder_, 0);
    }
    else if (root_ == VALUE_ARGUMENTS) {
      result_ = valueArguments(builder_, 0);
    }
    else if (root_ == VALUE_PARAMETER_LIST) {
      result_ = valueParameterList(builder_, 0);
    }
    else if (root_ == VALUE_PARAMETER_NO_TYPE) {
      result_ = valueParameterNoType(builder_, 0);
    }
    else if (root_ == VALUE_PARAMETER_WITH_TYPE) {
      result_ = valueParameterWithType(builder_, 0);
    }
    else if (root_ == VALUE_PARAMETERS) {
      result_ = valueParameters(builder_, 0);
    }
    else if (root_ == VARIABLE_DECLARATION_ENTRY_EXT) {
      result_ = variableDeclarationEntryExt(builder_, 0);
    }
    else if (root_ == WHEN) {
      result_ = when(builder_, 0);
    }
    else if (root_ == WHEN_CONDITION_EXPRESSION) {
      result_ = whenConditionExpression(builder_, 0);
    }
    else if (root_ == WHEN_CONDITION_IN_RANGE) {
      result_ = whenConditionInRange(builder_, 0);
    }
    else if (root_ == WHEN_CONDITION_IS_PATTERN) {
      result_ = whenConditionIsPattern(builder_, 0);
    }
    else if (root_ == WHEN_ENTRY) {
      result_ = whenEntry(builder_, 0);
    }
    else if (root_ == WHEN_PROPERTY) {
      result_ = whenProperty(builder_, 0);
    }
    else if (root_ == WHILE_LOOP) {
      result_ = whileLoop(builder_, 0);
    }
    else {
      result_ = parse_root_(root_, builder_, 0);
    }
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
    return builder_.getTreeBuilt();
  }

  protected boolean parse_root_(final IElementType root_, final PsiBuilder builder_, final int level_) {
    return root(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // "&&"
  public static boolean AND_OPERATION(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "AND_OPERATION")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<and operation>");
    result_ = consumeToken(builder_, "&&");
    exit_section_(builder_, level_, marker_, AND_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // CHARACTER_LITERAL
  public static boolean CHARACTER_CONSTANT(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "CHARACTER_CONSTANT")) return false;
    if (!nextTokenIs(builder_, CHARACTER_LITERAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, CHARACTER_LITERAL);
    exit_section_(builder_, marker_, CHARACTER_CONSTANT, result_);
    return result_;
  }

  /* ********************************************************** */
  // "." referenceExpression
  public static boolean DOT_IDENTIFIER(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "DOT_IDENTIFIER")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<dot identifier>");
    result_ = consumeToken(builder_, ".");
    result_ = result_ && referenceExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, DOT_IDENTIFIER, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // FLOAT_LITERAL
  public static boolean FLOAT_CONSTANT(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "FLOAT_CONSTANT")) return false;
    if (!nextTokenIs(builder_, FLOAT_LITERAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, FLOAT_LITERAL);
    exit_section_(builder_, marker_, FLOAT_CONSTANT, result_);
    return result_;
  }

  /* ********************************************************** */
  // referenceExpression DOT_IDENTIFIER*
  static boolean IDENTIFIER_EXT(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "IDENTIFIER_EXT")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && IDENTIFIER_EXT_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // DOT_IDENTIFIER*
  private static boolean IDENTIFIER_EXT_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "IDENTIFIER_EXT_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!DOT_IDENTIFIER(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "IDENTIFIER_EXT_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // INTEGER_LITERAL
  public static boolean INTEGER_CONSTANT(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "INTEGER_CONSTANT")) return false;
    if (!nextTokenIs(builder_, INTEGER_LITERAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, INTEGER_LITERAL);
    exit_section_(builder_, marker_, INTEGER_CONSTANT, result_);
    return result_;
  }

  /* ********************************************************** */
  // ";"+ | NEWLINE_BEFORE_CURRENT_TOKEN
  static boolean MULTISEMI(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "MULTISEMI")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = MULTISEMI_0(builder_, level_ + 1);
    if (!result_) result_ = newlineBeforeCurrentToken(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ";"+
  private static boolean MULTISEMI_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "MULTISEMI_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ";");
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!consumeToken(builder_, ";")) break;
      if (!empty_element_parsed_guard_(builder_, "MULTISEMI_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "null"
  public static boolean NULL(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "NULL")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<null>");
    result_ = consumeToken(builder_, "null");
    exit_section_(builder_, level_, marker_, NULL, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "||"
  public static boolean OR_OPERATION(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "OR_OPERATION")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<or operation>");
    result_ = consumeToken(builder_, "||");
    exit_section_(builder_, level_, marker_, OR_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // ";" | NEWLINE_BEFORE_CURRENT_TOKEN
  static boolean SEMI(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "SEMI")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ";");
    if (!result_) result_ = newlineBeforeCurrentToken(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<at PRIVATE_KEYWORD>> "private"
  //   | <<at PROTECTED_KEYWORD>> "protected"
  //   | <<at PUBLIC_KEYWORD>> "public"
  //   | <<at INTERNAL_KEYWORD>> "internal"
  static boolean accessModifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "accessModifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = accessModifier_0(builder_, level_ + 1);
    if (!result_) result_ = accessModifier_1(builder_, level_ + 1);
    if (!result_) result_ = accessModifier_2(builder_, level_ + 1);
    if (!result_) result_ = accessModifier_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at PRIVATE_KEYWORD>> "private"
  private static boolean accessModifier_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "accessModifier_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, PRIVATE_KEYWORD);
    result_ = result_ && consumeToken(builder_, "private");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at PROTECTED_KEYWORD>> "protected"
  private static boolean accessModifier_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "accessModifier_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, PROTECTED_KEYWORD);
    result_ = result_ && consumeToken(builder_, "protected");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at PUBLIC_KEYWORD>> "public"
  private static boolean accessModifier_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "accessModifier_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, PUBLIC_KEYWORD);
    result_ = result_ && consumeToken(builder_, "public");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at INTERNAL_KEYWORD>> "internal"
  private static boolean accessModifier_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "accessModifier_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, INTERNAL_KEYWORD);
    result_ = result_ && consumeToken(builder_, "internal");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // multiplicativeExpression additiveExpressionPlus*
  static boolean additiveExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = multiplicativeExpression(builder_, level_ + 1);
    result_ = result_ && additiveExpression_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // additiveExpressionPlus*
  private static boolean additiveExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!additiveExpressionPlus(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "additiveExpression_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE additiveOperation multiplicativeExpression
  public static boolean additiveExpressionPlus(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpressionPlus")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<additive expression plus>");
    result_ = additiveExpressionPlus_0(builder_, level_ + 1);
    result_ = result_ && additiveOperation(builder_, level_ + 1);
    result_ = result_ && multiplicativeExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ADDITIVE_EXPRESSION_PLUS, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean additiveExpressionPlus_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpressionPlus_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "+" | "-"
  public static boolean additiveOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<additive operation>");
    result_ = consumeToken(builder_, "+");
    if (!result_) result_ = consumeToken(builder_, "-");
    exit_section_(builder_, level_, marker_, ADDITIVE_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // annotation prefixUnaryExpression
  public static boolean annotatedExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "annotatedExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<annotated expression>");
    result_ = annotation(builder_, level_ + 1);
    result_ = result_ && prefixUnaryExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ANNOTATED_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // longAnnotation
  //   | <<shortAnnotationsAvailable>> annotationEntry
  static boolean annotation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "annotation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = longAnnotation(builder_, level_ + 1);
    if (!result_) result_ = annotation_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<shortAnnotationsAvailable>> annotationEntry
  private static boolean annotation_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "annotation_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = shortAnnotationsAvailable(builder_, level_ + 1);
    result_ = result_ && annotationEntry(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // FORBID_SHORT_ANNOTATIONS constructorCallee typeArguments? valueArguments? RESTORE_ANNOTATIONS_STATE
  public static boolean annotationEntry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "annotationEntry")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<annotation entry>");
    result_ = forbidShortAnnotations(builder_, level_ + 1, marker_);
    result_ = result_ && constructorCallee(builder_, level_ + 1);
    result_ = result_ && annotationEntry_2(builder_, level_ + 1);
    result_ = result_ && annotationEntry_3(builder_, level_ + 1);
    result_ = result_ && restoreAnnotationsState(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ANNOTATION_ENTRY, result_, false, null);
    return result_;
  }

  // typeArguments?
  private static boolean annotationEntry_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "annotationEntry_2")) return false;
    typeArguments(builder_, level_ + 1);
    return true;
  }

  // valueArguments?
  private static boolean annotationEntry_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "annotationEntry_3")) return false;
    valueArguments(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // annotation*
  public static boolean annotations(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "annotations")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<annotations>");
    int pos_ = current_position_(builder_);
    while (true) {
      if (!annotation(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "annotations", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, level_, marker_, ANNOTATIONS, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // annotation+
  static boolean annotationsPlus(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "annotationsPlus")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = annotation(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!annotation(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "annotationsPlus", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // modifierListWithShortAnnotations? block
  public static boolean anonymousInitializer(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousInitializer")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<anonymous initializer>");
    result_ = anonymousInitializer_0(builder_, level_ + 1);
    result_ = result_ && block(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ANONYMOUS_INITIALIZER, result_, false, null);
    return result_;
  }

  // modifierListWithShortAnnotations?
  private static boolean anonymousInitializer_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousInitializer_0")) return false;
    modifierListWithShortAnnotations(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // indices
  public static boolean arrayAccess(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "arrayAccess")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<array access>");
    result_ = indices(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ARRAY_ACCESS, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (disjunction !INTERRUPTED_WITH_NEWLINE assignmentOperator)+ disjunction
  public static boolean assignmentExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignmentExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<assignment expression>");
    result_ = assignmentExpression_0(builder_, level_ + 1);
    result_ = result_ && disjunction(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ASSIGNMENT_EXPRESSION, result_, false, null);
    return result_;
  }

  // (disjunction !INTERRUPTED_WITH_NEWLINE assignmentOperator)+
  private static boolean assignmentExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignmentExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = assignmentExpression_0_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!assignmentExpression_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "assignmentExpression_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // disjunction !INTERRUPTED_WITH_NEWLINE assignmentOperator
  private static boolean assignmentExpression_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignmentExpression_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = disjunction(builder_, level_ + 1);
    result_ = result_ && assignmentExpression_0_0_1(builder_, level_ + 1);
    result_ = result_ && assignmentOperator(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean assignmentExpression_0_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignmentExpression_0_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "="
  //   | "+=" | "-=" | "*=" | "/=" | "%="
  public static boolean assignmentOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignmentOperator")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<assignment operator>");
    result_ = consumeToken(builder_, "=");
    if (!result_) result_ = consumeToken(builder_, "+=");
    if (!result_) result_ = consumeToken(builder_, "-=");
    if (!result_) result_ = consumeToken(builder_, "*=");
    if (!result_) result_ = consumeToken(builder_, "/=");
    if (!result_) result_ = consumeToken(builder_, "%=");
    exit_section_(builder_, level_, marker_, ASSIGNMENT_OPERATOR, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "*"
  public static boolean asterisk(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "asterisk")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<asterisk>");
    result_ = consumeToken(builder_, "*");
    exit_section_(builder_, level_, marker_, ASTERISK, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // parenthesizedExpression
  //   | literalConstant
  //   | functionLiteralExpression
  //   | thisExpression label?
  //   | superExpression
  //   | ifExpression
  //   | when
  //   | tryBlock
  //   | objectLiteral
  //   | jump
  //   | loop
  //   | referenceExpression
  //   | fieldIdentifierReference
  //   | atomicPackage
  static boolean atomicExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "atomicExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parenthesizedExpression(builder_, level_ + 1);
    if (!result_) result_ = literalConstant(builder_, level_ + 1);
    if (!result_) result_ = functionLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = atomicExpression_3(builder_, level_ + 1);
    if (!result_) result_ = superExpression(builder_, level_ + 1);
    if (!result_) result_ = ifExpression(builder_, level_ + 1);
    if (!result_) result_ = when(builder_, level_ + 1);
    if (!result_) result_ = tryBlock(builder_, level_ + 1);
    if (!result_) result_ = objectLiteral(builder_, level_ + 1);
    if (!result_) result_ = jump(builder_, level_ + 1);
    if (!result_) result_ = loop(builder_, level_ + 1);
    if (!result_) result_ = referenceExpression(builder_, level_ + 1);
    if (!result_) result_ = fieldIdentifierReference(builder_, level_ + 1);
    if (!result_) result_ = atomicPackage(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // thisExpression label?
  private static boolean atomicExpression_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "atomicExpression_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = thisExpression(builder_, level_ + 1);
    result_ = result_ && atomicExpression_3_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // label?
  private static boolean atomicExpression_3_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "atomicExpression_3_1")) return false;
    label(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // atomicExpression | callableReference
  static boolean atomicOrCallable(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "atomicOrCallable")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = atomicExpression(builder_, level_ + 1);
    if (!result_) result_ = callableReference(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "package"
  public static boolean atomicPackage(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "atomicPackage")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<atomic package>");
    result_ = consumeToken(builder_, "package");
    exit_section_(builder_, level_, marker_, ATOMIC_PACKAGE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "true" | "false"
  public static boolean binaryConstant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "binaryConstant")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<binary constant>");
    result_ = consumeToken(builder_, "true");
    if (!result_) result_ = consumeToken(builder_, "false");
    exit_section_(builder_, level_, marker_, BINARY_CONSTANT, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "{" ENABLE_NEWLINES statements RESTORE_NEWLINES_STATE"}"
  public static boolean block(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<block>");
    result_ = consumeToken(builder_, "{");
    result_ = result_ && enableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && statements(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "}");
    exit_section_(builder_, level_, marker_, BLOCK, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (block | expression)?
  public static boolean body(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "body")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<body>");
    body_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, BODY, true, false, null);
    return true;
  }

  // block | expression
  private static boolean body_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "body_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = block(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // callWithClosure+
  //   | typeArgumentList (!INTERRUPTED_WITH_NEWLINE valueArguments)? callWithClosure*
  //   | valueArguments callWithClosure*
  public static boolean callSuffix(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<call suffix>");
    result_ = callSuffix_0(builder_, level_ + 1);
    if (!result_) result_ = callSuffix_1(builder_, level_ + 1);
    if (!result_) result_ = callSuffix_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CALL_SUFFIX, result_, false, null);
    return result_;
  }

  // callWithClosure+
  private static boolean callSuffix_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callWithClosure(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!callWithClosure(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "callSuffix_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeArgumentList (!INTERRUPTED_WITH_NEWLINE valueArguments)? callWithClosure*
  private static boolean callSuffix_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeArgumentList(builder_, level_ + 1);
    result_ = result_ && callSuffix_1_1(builder_, level_ + 1);
    result_ = result_ && callSuffix_1_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (!INTERRUPTED_WITH_NEWLINE valueArguments)?
  private static boolean callSuffix_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix_1_1")) return false;
    callSuffix_1_1_0(builder_, level_ + 1);
    return true;
  }

  // !INTERRUPTED_WITH_NEWLINE valueArguments
  private static boolean callSuffix_1_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix_1_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callSuffix_1_1_0_0(builder_, level_ + 1);
    result_ = result_ && valueArguments(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean callSuffix_1_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix_1_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // callWithClosure*
  private static boolean callSuffix_1_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix_1_2")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!callWithClosure(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "callSuffix_1_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // valueArguments callWithClosure*
  private static boolean callSuffix_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = valueArguments(builder_, level_ + 1);
    result_ = result_ && callSuffix_2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // callWithClosure*
  private static boolean callSuffix_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix_2_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!callWithClosure(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "callSuffix_2_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // typeArguments? valueArguments (label? )//functionLiteral?)
  //   | typeArguments (label? functionLiteral?)
  static boolean callSuffix2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callSuffix2_0(builder_, level_ + 1);
    if (!result_) result_ = callSuffix2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeArguments? valueArguments (label? )
  private static boolean callSuffix2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callSuffix2_0_0(builder_, level_ + 1);
    result_ = result_ && valueArguments(builder_, level_ + 1);
    result_ = result_ && callSuffix2_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeArguments?
  private static boolean callSuffix2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix2_0_0")) return false;
    typeArguments(builder_, level_ + 1);
    return true;
  }

  // label?
  private static boolean callSuffix2_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix2_0_2")) return false;
    label(builder_, level_ + 1);
    return true;
  }

  // typeArguments (label? functionLiteral?)
  private static boolean callSuffix2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix2_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeArguments(builder_, level_ + 1);
    result_ = result_ && callSuffix2_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // label? functionLiteral?
  private static boolean callSuffix2_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix2_1_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callSuffix2_1_1_0(builder_, level_ + 1);
    result_ = result_ && callSuffix2_1_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // label?
  private static boolean callSuffix2_1_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix2_1_1_0")) return false;
    label(builder_, level_ + 1);
    return true;
  }

  // functionLiteral?
  private static boolean callSuffix2_1_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callSuffix2_1_1_1")) return false;
    functionLiteral(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // <<availableCallWithClosure>>
  //   (label | functionLiteralArgument)
  static boolean callWithClosure(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callWithClosure")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = availableCallWithClosure(builder_, level_ + 1);
    result_ = result_ && callWithClosure_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // label | functionLiteralArgument
  private static boolean callWithClosure_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callWithClosure_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = label(builder_, level_ + 1);
    if (!result_) result_ = functionLiteralArgument(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // userType? "::" IDENTIFIER
  public static boolean callableReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callableReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<callable reference>");
    result_ = callableReference_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "::");
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    exit_section_(builder_, level_, marker_, CALLABLE_REFERENCE, result_, false, null);
    return result_;
  }

  // userType?
  private static boolean callableReference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callableReference_0")) return false;
    userType(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // <<at CATCH_KEYWORD>> "catch"
  //   //"(" annotationsPlus? IDENTIFIER ":" userType ")" block
  //   singleValueParameterListWithBrackets block
  public static boolean catchBlock(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "catchBlock")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<catch block>");
    result_ = at(builder_, level_ + 1, CATCH_KEYWORD);
    result_ = result_ && consumeToken(builder_, "catch");
    result_ = result_ && singleValueParameterListWithBrackets(builder_, level_ + 1);
    result_ = result_ && block(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CATCH_BLOCK, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "{" ENABLE_NEWLINES memberDeclaration* RESTORE_NEWLINES_STATE"}"
  public static boolean classBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classBody")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<class body>");
    result_ = consumeToken(builder_, "{");
    result_ = result_ && enableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && classBody_2(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "}");
    exit_section_(builder_, level_, marker_, CLASS_BODY, result_, false, null);
    return result_;
  }

  // memberDeclaration*
  private static boolean classBody_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classBody_2")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!memberDeclaration(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "classBody_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // modifierListWithShortAnnotations? ("class" | "trait") IDENTIFIER
  //       typeParameters?
  //       primaryConstructorModifierList? valueParameters?
  //       (":" annotationsPlus? delegationSpecifierExt)?
  //       typeConstraints?
  //       (enumClassBody | classBody?)
  public static boolean classDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<class declaration>");
    result_ = classDeclaration_0(builder_, level_ + 1);
    result_ = result_ && classDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    result_ = result_ && classDeclaration_3(builder_, level_ + 1);
    result_ = result_ && classDeclaration_4(builder_, level_ + 1);
    result_ = result_ && classDeclaration_5(builder_, level_ + 1);
    result_ = result_ && classDeclaration_6(builder_, level_ + 1);
    result_ = result_ && classDeclaration_7(builder_, level_ + 1);
    result_ = result_ && classDeclaration_8(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CLASS_DECLARATION, result_, false, null);
    return result_;
  }

  // modifierListWithShortAnnotations?
  private static boolean classDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_0")) return false;
    modifierListWithShortAnnotations(builder_, level_ + 1);
    return true;
  }

  // "class" | "trait"
  private static boolean classDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "class");
    if (!result_) result_ = consumeToken(builder_, "trait");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeParameters?
  private static boolean classDeclaration_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_3")) return false;
    typeParameters(builder_, level_ + 1);
    return true;
  }

  // primaryConstructorModifierList?
  private static boolean classDeclaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_4")) return false;
    primaryConstructorModifierList(builder_, level_ + 1);
    return true;
  }

  // valueParameters?
  private static boolean classDeclaration_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_5")) return false;
    valueParameters(builder_, level_ + 1);
    return true;
  }

  // (":" annotationsPlus? delegationSpecifierExt)?
  private static boolean classDeclaration_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_6")) return false;
    classDeclaration_6_0(builder_, level_ + 1);
    return true;
  }

  // ":" annotationsPlus? delegationSpecifierExt
  private static boolean classDeclaration_6_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_6_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && classDeclaration_6_0_1(builder_, level_ + 1);
    result_ = result_ && delegationSpecifierExt(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean classDeclaration_6_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_6_0_1")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  // typeConstraints?
  private static boolean classDeclaration_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_7")) return false;
    typeConstraints(builder_, level_ + 1);
    return true;
  }

  // enumClassBody | classBody?
  private static boolean classDeclaration_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_8")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = enumClassBody(builder_, level_ + 1);
    if (!result_) result_ = classDeclaration_8_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // classBody?
  private static boolean classDeclaration_8_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_8_1")) return false;
    classBody(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // modifierList? ("class" | "trait") IDENTIFIER
  //       typeParameters?
  //       primaryConstructorModifierList? valueParameters?
  //       (":" annotationsPlus? delegationSpecifierExt)?
  //       typeConstraints?
  //       (enumClassBody | classBody?)
  public static boolean classDeclarationLocal(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<class declaration local>");
    result_ = classDeclarationLocal_0(builder_, level_ + 1);
    result_ = result_ && classDeclarationLocal_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    result_ = result_ && classDeclarationLocal_3(builder_, level_ + 1);
    result_ = result_ && classDeclarationLocal_4(builder_, level_ + 1);
    result_ = result_ && classDeclarationLocal_5(builder_, level_ + 1);
    result_ = result_ && classDeclarationLocal_6(builder_, level_ + 1);
    result_ = result_ && classDeclarationLocal_7(builder_, level_ + 1);
    result_ = result_ && classDeclarationLocal_8(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CLASS_DECLARATION_LOCAL, result_, false, null);
    return result_;
  }

  // modifierList?
  private static boolean classDeclarationLocal_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // "class" | "trait"
  private static boolean classDeclarationLocal_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "class");
    if (!result_) result_ = consumeToken(builder_, "trait");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeParameters?
  private static boolean classDeclarationLocal_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_3")) return false;
    typeParameters(builder_, level_ + 1);
    return true;
  }

  // primaryConstructorModifierList?
  private static boolean classDeclarationLocal_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_4")) return false;
    primaryConstructorModifierList(builder_, level_ + 1);
    return true;
  }

  // valueParameters?
  private static boolean classDeclarationLocal_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_5")) return false;
    valueParameters(builder_, level_ + 1);
    return true;
  }

  // (":" annotationsPlus? delegationSpecifierExt)?
  private static boolean classDeclarationLocal_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_6")) return false;
    classDeclarationLocal_6_0(builder_, level_ + 1);
    return true;
  }

  // ":" annotationsPlus? delegationSpecifierExt
  private static boolean classDeclarationLocal_6_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_6_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && classDeclarationLocal_6_0_1(builder_, level_ + 1);
    result_ = result_ && delegationSpecifierExt(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean classDeclarationLocal_6_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_6_0_1")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  // typeConstraints?
  private static boolean classDeclarationLocal_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_7")) return false;
    typeConstraints(builder_, level_ + 1);
    return true;
  }

  // enumClassBody | classBody?
  private static boolean classDeclarationLocal_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_8")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = enumClassBody(builder_, level_ + 1);
    if (!result_) result_ = classDeclarationLocal_8_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // classBody?
  private static boolean classDeclarationLocal_8_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclarationLocal_8_1")) return false;
    classBody(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // <<at ABSTRACT_KEYWORD>> "abstract"
  //   | <<at FINAL_KEYWORD>> "final"
  //   | <<at ENUM_KEYWORD>> "enum"
  //   | <<at OPEN_KEYWORD>> "open"
  //   | <<at ANNOTATION_KEYWORD>> "annotation"
  //   | <<at INNER_KEYWORD>> "inner"
  static boolean classModifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classModifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = classModifier_0(builder_, level_ + 1);
    if (!result_) result_ = classModifier_1(builder_, level_ + 1);
    if (!result_) result_ = classModifier_2(builder_, level_ + 1);
    if (!result_) result_ = classModifier_3(builder_, level_ + 1);
    if (!result_) result_ = classModifier_4(builder_, level_ + 1);
    if (!result_) result_ = classModifier_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at ABSTRACT_KEYWORD>> "abstract"
  private static boolean classModifier_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classModifier_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, ABSTRACT_KEYWORD);
    result_ = result_ && consumeToken(builder_, "abstract");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at FINAL_KEYWORD>> "final"
  private static boolean classModifier_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classModifier_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, FINAL_KEYWORD);
    result_ = result_ && consumeToken(builder_, "final");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at ENUM_KEYWORD>> "enum"
  private static boolean classModifier_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classModifier_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, ENUM_KEYWORD);
    result_ = result_ && consumeToken(builder_, "enum");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at OPEN_KEYWORD>> "open"
  private static boolean classModifier_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classModifier_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, OPEN_KEYWORD);
    result_ = result_ && consumeToken(builder_, "open");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at ANNOTATION_KEYWORD>> "annotation"
  private static boolean classModifier_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classModifier_4")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, ANNOTATION_KEYWORD);
    result_ = result_ && consumeToken(builder_, "annotation");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at INNER_KEYWORD>> "inner"
  private static boolean classModifier_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classModifier_5")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, INNER_KEYWORD);
    result_ = result_ && consumeToken(builder_, "inner");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // modifierListWithShortAnnotations? "class" objectUnnamed
  public static boolean classObject(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classObject")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<class object>");
    result_ = classObject_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "class");
    result_ = result_ && objectUnnamed(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CLASS_OBJECT, result_, false, null);
    return result_;
  }

  // modifierListWithShortAnnotations?
  private static boolean classObject_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classObject_0")) return false;
    modifierListWithShortAnnotations(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // namedInfix comparisonExpression*
  static boolean comparison(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "comparison")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = namedInfix(builder_, level_ + 1);
    result_ = result_ && comparison_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // comparisonExpression*
  private static boolean comparison_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "comparison_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!comparisonExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "comparison_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE comparisonOperation namedInfix
  public static boolean comparisonExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "comparisonExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<comparison expression>");
    result_ = comparisonExpression_0(builder_, level_ + 1);
    result_ = result_ && comparisonOperation(builder_, level_ + 1);
    result_ = result_ && namedInfix(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, COMPARISON_EXPRESSION, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean comparisonExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "comparisonExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "<" | ">" | ">=" | "<="
  public static boolean comparisonOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "comparisonOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<comparison operation>");
    result_ = consumeToken(builder_, "<");
    if (!result_) result_ = consumeToken(builder_, ">");
    if (!result_) result_ = consumeToken(builder_, ">=");
    if (!result_) result_ = consumeToken(builder_, "<=");
    exit_section_(builder_, level_, marker_, COMPARISON_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // expression
  public static boolean condition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "condition")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<condition>");
    result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CONDITION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // equalityComparison conjunctionPlus*
  static boolean conjunction(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "conjunction")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = equalityComparison(builder_, level_ + 1);
    result_ = result_ && conjunction_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // conjunctionPlus*
  private static boolean conjunction_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "conjunction_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!conjunctionPlus(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "conjunction_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE AND_OPERATION equalityComparison
  public static boolean conjunctionPlus(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "conjunctionPlus")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<conjunction plus>");
    result_ = conjunctionPlus_0(builder_, level_ + 1);
    result_ = result_ && AND_OPERATION(builder_, level_ + 1);
    result_ = result_ && equalityComparison(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CONJUNCTION_PLUS, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean conjunctionPlus_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "conjunctionPlus_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // userTypeReference
  public static boolean constructorCallee(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "constructorCallee")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<constructor callee>");
    result_ = userTypeReference(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CONSTRUCTOR_CALLEE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // constructorCallee callSuffix2
  static boolean constructorInvocation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "constructorInvocation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = constructorCallee(builder_, level_ + 1);
    result_ = result_ && callSuffix2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // function
  //   | propertyLocal
  //   | classDeclarationLocal
  //   | typedef
  //   | object
  static boolean declaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "declaration")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = function(builder_, level_ + 1);
    if (!result_) result_ = propertyLocal(builder_, level_ + 1);
    if (!result_) result_ = classDeclarationLocal(builder_, level_ + 1);
    if (!result_) result_ = typedef(builder_, level_ + 1);
    if (!result_) result_ = object(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // delegatorSuperCall
  //   | explicitDelegation
  //   | delegatorSuperClass
  static boolean delegationSpecifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "delegationSpecifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = delegatorSuperCall(builder_, level_ + 1);
    if (!result_) result_ = explicitDelegation(builder_, level_ + 1);
    if (!result_) result_ = delegatorSuperClass(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (delegationSpecifier ",")* delegationSpecifier
  public static boolean delegationSpecifierExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "delegationSpecifierExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<delegation specifier ext>");
    result_ = delegationSpecifierExt_0(builder_, level_ + 1);
    result_ = result_ && delegationSpecifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, DELEGATION_SPECIFIER_EXT, result_, false, null);
    return result_;
  }

  // (delegationSpecifier ",")*
  private static boolean delegationSpecifierExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "delegationSpecifierExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!delegationSpecifierExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "delegationSpecifierExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // delegationSpecifier ","
  private static boolean delegationSpecifierExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "delegationSpecifierExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = delegationSpecifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // constructorInvocation
  public static boolean delegatorSuperCall(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "delegatorSuperCall")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<delegator super call>");
    result_ = constructorInvocation(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, DELEGATOR_SUPER_CALL, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // userTypeReference
  public static boolean delegatorSuperClass(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "delegatorSuperClass")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<delegator super class>");
    result_ = userTypeReference(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, DELEGATOR_SUPER_CLASS, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // conjunction disjunctionPlus*
  static boolean disjunction(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "disjunction")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = conjunction(builder_, level_ + 1);
    result_ = result_ && disjunction_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // disjunctionPlus*
  private static boolean disjunction_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "disjunction_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!disjunctionPlus(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "disjunction_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE OR_OPERATION conjunction
  public static boolean disjunctionPlus(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "disjunctionPlus")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<disjunction plus>");
    result_ = disjunctionPlus_0(builder_, level_ + 1);
    result_ = result_ && OR_OPERATION(builder_, level_ + 1);
    result_ = result_ && conjunction(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, DISJUNCTION_PLUS, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean disjunctionPlus_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "disjunctionPlus_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "do" (!<<next WHILE_KEYWORD>> body)? "while" "(" condition ")"
  public static boolean doWhileLoop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "doWhileLoop")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<do while loop>");
    result_ = consumeToken(builder_, "do");
    result_ = result_ && doWhileLoop_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "while");
    result_ = result_ && consumeToken(builder_, "(");
    result_ = result_ && condition(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, level_, marker_, DO_WHILE_LOOP, result_, false, null);
    return result_;
  }

  // (!<<next WHILE_KEYWORD>> body)?
  private static boolean doWhileLoop_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "doWhileLoop_1")) return false;
    doWhileLoop_1_0(builder_, level_ + 1);
    return true;
  }

  // !<<next WHILE_KEYWORD>> body
  private static boolean doWhileLoop_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "doWhileLoop_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = doWhileLoop_1_0_0(builder_, level_ + 1);
    result_ = result_ && body(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !<<next WHILE_KEYWORD>>
  private static boolean doWhileLoop_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "doWhileLoop_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !next(builder_, level_ + 1, WHILE_KEYWORD);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE "." postfixUnaryExpression2
  public static boolean dotQualifiedExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dotQualifiedExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<dot qualified expression>");
    result_ = dotQualifiedExpression_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ".");
    result_ = result_ && postfixUnaryExpression2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, DOT_QUALIFIED_EXPRESSION, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean dotQualifiedExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dotQualifiedExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (block |expression)?
  public static boolean elseExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "elseExpression")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<else expression>");
    elseExpression_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ELSE_EXPRESSION, true, false, null);
    return true;
  }

  // block |expression
  private static boolean elseExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "elseExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = block(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // elvisOperation (atomicExpression | callableReference)
  public static boolean elvisAccessExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "elvisAccessExpression")) return false;
    if (!nextTokenIs(builder_, ELVIS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = elvisOperation(builder_, level_ + 1);
    result_ = result_ && elvisAccessExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ELVIS_ACCESS_EXPRESSION, result_, false, null);
    return result_;
  }

  // atomicExpression | callableReference
  private static boolean elvisAccessExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "elvisAccessExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = atomicExpression(builder_, level_ + 1);
    if (!result_) result_ = callableReference(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // infixFunctionCall (!INTERRUPTED_WITH_NEWLINE "?:" infixFunctionCall)*
  static boolean elvisExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "elvisExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = infixFunctionCall(builder_, level_ + 1);
    result_ = result_ && elvisExpression_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (!INTERRUPTED_WITH_NEWLINE "?:" infixFunctionCall)*
  private static boolean elvisExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "elvisExpression_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!elvisExpression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "elvisExpression_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // !INTERRUPTED_WITH_NEWLINE "?:" infixFunctionCall
  private static boolean elvisExpression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "elvisExpression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = elvisExpression_1_0_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "?:");
    result_ = result_ && infixFunctionCall(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean elvisExpression_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "elvisExpression_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // ELVIS
  public static boolean elvisOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "elvisOperation")) return false;
    if (!nextTokenIs(builder_, ELVIS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ELVIS);
    exit_section_(builder_, marker_, ELVIS_OPERATION, result_);
    return result_;
  }

  /* ********************************************************** */
  // "{" ENABLE_NEWLINES (enumEntry | memberDeclaration)* RESTORE_NEWLINES_STATE"}"
  public static boolean enumClassBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumClassBody")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<enum class body>");
    result_ = consumeToken(builder_, "{");
    result_ = result_ && enableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && enumClassBody_2(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "}");
    exit_section_(builder_, level_, marker_, ENUM_CLASS_BODY, result_, false, null);
    return result_;
  }

  // (enumEntry | memberDeclaration)*
  private static boolean enumClassBody_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumClassBody_2")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!enumClassBody_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "enumClassBody_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // enumEntry | memberDeclaration
  private static boolean enumClassBody_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumClassBody_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = enumEntry(builder_, level_ + 1);
    if (!result_) result_ = memberDeclaration(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // modifierList? objectDeclarationName (":" initializerExt)? classBody? SEMI?
  public static boolean enumEntry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumEntry")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<enum entry>");
    result_ = enumEntry_0(builder_, level_ + 1);
    result_ = result_ && objectDeclarationName(builder_, level_ + 1);
    result_ = result_ && enumEntry_2(builder_, level_ + 1);
    result_ = result_ && enumEntry_3(builder_, level_ + 1);
    result_ = result_ && enumEntry_4(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ENUM_ENTRY, result_, false, null);
    return result_;
  }

  // modifierList?
  private static boolean enumEntry_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumEntry_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // (":" initializerExt)?
  private static boolean enumEntry_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumEntry_2")) return false;
    enumEntry_2_0(builder_, level_ + 1);
    return true;
  }

  // ":" initializerExt
  private static boolean enumEntry_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumEntry_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && initializerExt(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // classBody?
  private static boolean enumEntry_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumEntry_3")) return false;
    classBody(builder_, level_ + 1);
    return true;
  }

  // SEMI?
  private static boolean enumEntry_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumEntry_4")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // comparison equalityComparisonExpression*
  static boolean equalityComparison(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equalityComparison")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = comparison(builder_, level_ + 1);
    result_ = result_ && equalityComparison_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // equalityComparisonExpression*
  private static boolean equalityComparison_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equalityComparison_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!equalityComparisonExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "equalityComparison_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE equalityOperation comparison
  public static boolean equalityComparisonExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equalityComparisonExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<equality comparison expression>");
    result_ = equalityComparisonExpression_0(builder_, level_ + 1);
    result_ = result_ && equalityOperation(builder_, level_ + 1);
    result_ = result_ && comparison(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, EQUALITY_COMPARISON_EXPRESSION, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean equalityComparisonExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equalityComparisonExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "!=" | "==" | "==="
  public static boolean equalityOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equalityOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<equality operation>");
    result_ = consumeToken(builder_, "!=");
    if (!result_) result_ = consumeToken(builder_, "==");
    if (!result_) result_ = consumeToken(builder_, "===");
    exit_section_(builder_, level_, marker_, EQUALITY_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // userTypeReference <<at BY_KEYWORD>> "by"
  //   ADD_NEW_CALL_WITH_CLOSURE_COUNTER
  //   expression
  //   DELETE_CALL_WITH_CLOSURE_COUNTER
  public static boolean explicitDelegation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "explicitDelegation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<explicit delegation>");
    result_ = userTypeReference(builder_, level_ + 1);
    result_ = result_ && at(builder_, level_ + 1, BY_KEYWORD);
    result_ = result_ && consumeToken(builder_, "by");
    result_ = result_ && addNewCallWithClosureCounter(builder_, level_ + 1, marker_);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && deleteCallWithClosureCounter(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, EXPLICIT_DELEGATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // assignmentExpression
  //   | disjunction
  static boolean expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = assignmentExpression(builder_, level_ + 1);
    if (!result_) result_ = disjunction(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (expression ",")* expression
  static boolean expressionExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = expressionExt_0(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (expression ",")*
  private static boolean expressionExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!expressionExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "expressionExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // expression ","
  private static boolean expressionExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // FIELD_IDENTIFIER
  public static boolean fieldIdentifierReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "fieldIdentifierReference")) return false;
    if (!nextTokenIs(builder_, FIELD_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, FIELD_IDENTIFIER);
    exit_section_(builder_, marker_, FIELD_IDENTIFIER_REFERENCE, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<at FINALLY_KEYWORD>> "finally" block
  public static boolean finallyBlock(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "finallyBlock")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<finally block>");
    result_ = at(builder_, level_ + 1, FINALLY_KEYWORD);
    result_ = result_ && consumeToken(builder_, "finally");
    result_ = result_ && block(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FINALLY_BLOCK, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "for" "(" forValueParameter "in" loopRange ")" body
  public static boolean forLoop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forLoop")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<for loop>");
    result_ = consumeToken(builder_, "for");
    result_ = result_ && consumeToken(builder_, "(");
    result_ = result_ && forValueParameter(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "in");
    result_ = result_ && loopRange(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    result_ = result_ && body(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FOR_LOOP, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // annotationsPlus? ("val" | "var")? (multipleVariableDeclarations | variableDeclarationEntry)
  public static boolean forValueParameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forValueParameter")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<for value parameter>");
    result_ = forValueParameter_0(builder_, level_ + 1);
    result_ = result_ && forValueParameter_1(builder_, level_ + 1);
    result_ = result_ && forValueParameter_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FOR_VALUE_PARAMETER, result_, false, null);
    return result_;
  }

  // annotationsPlus?
  private static boolean forValueParameter_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forValueParameter_0")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  // ("val" | "var")?
  private static boolean forValueParameter_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forValueParameter_1")) return false;
    forValueParameter_1_0(builder_, level_ + 1);
    return true;
  }

  // "val" | "var"
  private static boolean forValueParameter_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forValueParameter_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "val");
    if (!result_) result_ = consumeToken(builder_, "var");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // multipleVariableDeclarations | variableDeclarationEntry
  private static boolean forValueParameter_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forValueParameter_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = multipleVariableDeclarations(builder_, level_ + 1);
    if (!result_) result_ = variableDeclarationEntry(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // modifierListWithShortAnnotations? "fun" typeParameters?
  //       ( DISABLE_JOINING_COMPLEX_TOKENS <<stopAtLastDot marker_>>
  //       type <<unStop>> "." RESTORE_JOINING_COMPLEX_TOKENS_STATE
  //       | annotationsPlus)?
  //       IDENTIFIER
  //       typeParameters? valueParameters (":" type)?
  //       typeConstraints?
  //       functionBody? SEMI?
  public static boolean function(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function>");
    result_ = function_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "fun");
    result_ = result_ && function_2(builder_, level_ + 1);
    result_ = result_ && function_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    result_ = result_ && function_5(builder_, level_ + 1);
    result_ = result_ && valueParameters(builder_, level_ + 1);
    result_ = result_ && function_7(builder_, level_ + 1);
    result_ = result_ && function_8(builder_, level_ + 1);
    result_ = result_ && function_9(builder_, level_ + 1);
    result_ = result_ && function_10(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION, result_, false, null);
    return result_;
  }

  // modifierListWithShortAnnotations?
  private static boolean function_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_0")) return false;
    modifierListWithShortAnnotations(builder_, level_ + 1);
    return true;
  }

  // typeParameters?
  private static boolean function_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_2")) return false;
    typeParameters(builder_, level_ + 1);
    return true;
  }

  // ( DISABLE_JOINING_COMPLEX_TOKENS <<stopAtLastDot marker_>>
  //       type <<unStop>> "." RESTORE_JOINING_COMPLEX_TOKENS_STATE
  //       | annotationsPlus)?
  private static boolean function_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_3")) return false;
    function_3_0(builder_, level_ + 1);
    return true;
  }

  // DISABLE_JOINING_COMPLEX_TOKENS <<stopAtLastDot marker_>>
  //       type <<unStop>> "." RESTORE_JOINING_COMPLEX_TOKENS_STATE
  //       | annotationsPlus
  private static boolean function_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = function_3_0_0(builder_, level_ + 1);
    if (!result_) result_ = annotationsPlus(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // DISABLE_JOINING_COMPLEX_TOKENS <<stopAtLastDot marker_>>
  //       type <<unStop>> "." RESTORE_JOINING_COMPLEX_TOKENS_STATE
  private static boolean function_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_3_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = disableJoiningComplexTokens(builder_, level_ + 1, marker_);
    result_ = result_ && stopAtLastDot(builder_, level_ + 1, marker_);
    result_ = result_ && type(builder_, level_ + 1);
    result_ = result_ && unStop(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ".");
    result_ = result_ && restoreJoiningComplexTokensState(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeParameters?
  private static boolean function_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_5")) return false;
    typeParameters(builder_, level_ + 1);
    return true;
  }

  // (":" type)?
  private static boolean function_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_7")) return false;
    function_7_0(builder_, level_ + 1);
    return true;
  }

  // ":" type
  private static boolean function_7_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_7_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeConstraints?
  private static boolean function_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_8")) return false;
    typeConstraints(builder_, level_ + 1);
    return true;
  }

  // functionBody?
  private static boolean function_9(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_9")) return false;
    functionBody(builder_, level_ + 1);
    return true;
  }

  // SEMI?
  private static boolean function_10(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "function_10")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // block
  //   | "=" expression
  static boolean functionBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionBody")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = block(builder_, level_ + 1);
    if (!result_) result_ = functionBody_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "=" expression
  private static boolean functionBody_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionBody_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "=");
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // ENABLE_NEWLINES(
  //   "{" functionLiteralValueParameterList1 "->" statementsBlock "}"
  //   | "{" (type ".")? functionLiteralValueParameterList2 (":" type)? "->" statementsBlock "}"
  //   | "{" statementsBlock "}"
  //   ) RESTORE_NEWLINES_STATE
  public static boolean functionLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function literal>");
    result_ = enableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && functionLiteral_1(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION_LITERAL, result_, false, null);
    return result_;
  }

  // "{" functionLiteralValueParameterList1 "->" statementsBlock "}"
  //   | "{" (type ".")? functionLiteralValueParameterList2 (":" type)? "->" statementsBlock "}"
  //   | "{" statementsBlock "}"
  private static boolean functionLiteral_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionLiteral_1_0(builder_, level_ + 1);
    if (!result_) result_ = functionLiteral_1_1(builder_, level_ + 1);
    if (!result_) result_ = functionLiteral_1_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "{" functionLiteralValueParameterList1 "->" statementsBlock "}"
  private static boolean functionLiteral_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "{");
    result_ = result_ && functionLiteralValueParameterList1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "->");
    result_ = result_ && statementsBlock(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "}");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "{" (type ".")? functionLiteralValueParameterList2 (":" type)? "->" statementsBlock "}"
  private static boolean functionLiteral_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_1_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "{");
    result_ = result_ && functionLiteral_1_1_1(builder_, level_ + 1);
    result_ = result_ && functionLiteralValueParameterList2(builder_, level_ + 1);
    result_ = result_ && functionLiteral_1_1_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "->");
    result_ = result_ && statementsBlock(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "}");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (type ".")?
  private static boolean functionLiteral_1_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_1_1_1")) return false;
    functionLiteral_1_1_1_0(builder_, level_ + 1);
    return true;
  }

  // type "."
  private static boolean functionLiteral_1_1_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_1_1_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = type(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ".");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (":" type)?
  private static boolean functionLiteral_1_1_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_1_1_3")) return false;
    functionLiteral_1_1_3_0(builder_, level_ + 1);
    return true;
  }

  // ":" type
  private static boolean functionLiteral_1_1_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_1_1_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "{" statementsBlock "}"
  private static boolean functionLiteral_1_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_1_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "{");
    result_ = result_ && statementsBlock(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "}");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // functionLiteralExpression
  public static boolean functionLiteralArgument(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteralArgument")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function literal argument>");
    result_ = functionLiteralExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION_LITERAL_ARGUMENT, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // functionLiteral
  public static boolean functionLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteralExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function literal expression>");
    result_ = functionLiteral(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION_LITERAL_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // modifiersIDENTIFIERExt?
  public static boolean functionLiteralValueParameterList1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteralValueParameterList1")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function literal value parameter list 1>");
    modifiersIDENTIFIERExt(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION_LITERAL_VALUE_PARAMETER_LIST_1, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // "(" DISABLE_NEWLINES modifiersIDENTIFIERTypeExt? RESTORE_NEWLINES_STATE")"
  public static boolean functionLiteralValueParameterList2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteralValueParameterList2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function literal value parameter list 2>");
    result_ = consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && functionLiteralValueParameterList2_2(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, level_, marker_, FUNCTION_LITERAL_VALUE_PARAMETER_LIST_2, result_, false, null);
    return result_;
  }

  // modifiersIDENTIFIERTypeExt?
  private static boolean functionLiteralValueParameterList2_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteralValueParameterList2_2")) return false;
    modifiersIDENTIFIERTypeExt(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // <<stopInFunctionParameter marker_>> modifierList? <<unStop>> ("val" | "var")? privateParameter ("=" expression)?
  public static boolean functionParameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionParameter")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function parameter>");
    result_ = stopInFunctionParameter(builder_, level_ + 1, marker_);
    result_ = result_ && functionParameter_1(builder_, level_ + 1);
    result_ = result_ && unStop(builder_, level_ + 1);
    result_ = result_ && functionParameter_3(builder_, level_ + 1);
    result_ = result_ && privateParameter(builder_, level_ + 1);
    result_ = result_ && functionParameter_5(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION_PARAMETER, result_, false, null);
    return result_;
  }

  // modifierList?
  private static boolean functionParameter_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionParameter_1")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // ("val" | "var")?
  private static boolean functionParameter_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionParameter_3")) return false;
    functionParameter_3_0(builder_, level_ + 1);
    return true;
  }

  // "val" | "var"
  private static boolean functionParameter_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionParameter_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "val");
    if (!result_) result_ = consumeToken(builder_, "var");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ("=" expression)?
  private static boolean functionParameter_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionParameter_5")) return false;
    functionParameter_5_0(builder_, level_ + 1);
    return true;
  }

  // "=" expression
  private static boolean functionParameter_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionParameter_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "=");
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (functionParameter ",")* functionParameter
  static boolean functionParameterExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionParameterExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionParameterExt_0(builder_, level_ + 1);
    result_ = result_ && functionParameter(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (functionParameter ",")*
  private static boolean functionParameterExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionParameterExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!functionParameterExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "functionParameterExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // functionParameter ","
  private static boolean functionParameterExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionParameterExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionParameter(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // valueParameterList "->" type
  public static boolean functionType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function type>");
    result_ = valueParameterList(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "->");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION_TYPE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (typeDescriptorInBrackets
  //          | annotationsPlus? userType
  //          | selfType) nullableType*
  public static boolean functionTypeReceiver(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeReceiver")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function type receiver>");
    result_ = functionTypeReceiver_0(builder_, level_ + 1);
    result_ = result_ && functionTypeReceiver_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION_TYPE_RECEIVER, result_, false, null);
    return result_;
  }

  // typeDescriptorInBrackets
  //          | annotationsPlus? userType
  //          | selfType
  private static boolean functionTypeReceiver_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeReceiver_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeDescriptorInBrackets(builder_, level_ + 1);
    if (!result_) result_ = functionTypeReceiver_0_1(builder_, level_ + 1);
    if (!result_) result_ = selfType(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus? userType
  private static boolean functionTypeReceiver_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeReceiver_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionTypeReceiver_0_1_0(builder_, level_ + 1);
    result_ = result_ && userType(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean functionTypeReceiver_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeReceiver_0_1_0")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  // nullableType*
  private static boolean functionTypeReceiver_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeReceiver_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!nullableType(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "functionTypeReceiver_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // functionTypeReceiver
  public static boolean functionTypeReceiverReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeReceiverReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function type receiver reference>");
    result_ = functionTypeReceiver(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION_TYPE_RECEIVER_REFERENCE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (
  //     modifierList? <<at GET_KEYWORD>> "get"  "(" DISABLE_NEWLINES RESTORE_NEWLINES_STATE ")" (":" type)? functionBody
  //   | modifierList? <<at GET_KEYWORD>>  "get"
  //        ) SEMI?
  public static boolean getter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getter")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<getter>");
    result_ = getter_0(builder_, level_ + 1);
    result_ = result_ && getter_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, GETTER, result_, false, null);
    return result_;
  }

  // modifierList? <<at GET_KEYWORD>> "get"  "(" DISABLE_NEWLINES RESTORE_NEWLINES_STATE ")" (":" type)? functionBody
  //   | modifierList? <<at GET_KEYWORD>>  "get"
  private static boolean getter_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getter_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = getter_0_0(builder_, level_ + 1);
    if (!result_) result_ = getter_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // modifierList? <<at GET_KEYWORD>> "get"  "(" DISABLE_NEWLINES RESTORE_NEWLINES_STATE ")" (":" type)? functionBody
  private static boolean getter_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getter_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = getter_0_0_0(builder_, level_ + 1);
    result_ = result_ && at(builder_, level_ + 1, GET_KEYWORD);
    result_ = result_ && consumeToken(builder_, "get");
    result_ = result_ && consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    result_ = result_ && getter_0_0_7(builder_, level_ + 1);
    result_ = result_ && functionBody(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // modifierList?
  private static boolean getter_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getter_0_0_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // (":" type)?
  private static boolean getter_0_0_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getter_0_0_7")) return false;
    getter_0_0_7_0(builder_, level_ + 1);
    return true;
  }

  // ":" type
  private static boolean getter_0_0_7_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getter_0_0_7_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // modifierList? <<at GET_KEYWORD>>  "get"
  private static boolean getter_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getter_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = getter_0_1_0(builder_, level_ + 1);
    result_ = result_ && at(builder_, level_ + 1, GET_KEYWORD);
    result_ = result_ && consumeToken(builder_, "get");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // modifierList?
  private static boolean getter_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getter_0_1_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // SEMI?
  private static boolean getter_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getter_1")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "if" "(" DISABLE_NEWLINES condition RESTORE_NEWLINES_STATE ")"
  //       (
  //           thenExpressionWithSemi "else"  elseExpression
  //           | thenExpression
  //       )
  public static boolean ifExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<if expression>");
    result_ = consumeToken(builder_, "if");
    result_ = result_ && consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && condition(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    result_ = result_ && ifExpression_6(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, IF_EXPRESSION, result_, false, null);
    return result_;
  }

  // thenExpressionWithSemi "else"  elseExpression
  //           | thenExpression
  private static boolean ifExpression_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifExpression_6")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ifExpression_6_0(builder_, level_ + 1);
    if (!result_) result_ = thenExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // thenExpressionWithSemi "else"  elseExpression
  private static boolean ifExpression_6_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifExpression_6_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = thenExpressionWithSemi(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "else");
    result_ = result_ && elseExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<at IMPORT_KEYWORD>> "import" IDENTIFIER_EXT ("." "*" | "as" IDENTIFIER)? SEMI?
  public static boolean importDirective(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importDirective")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<import directive>");
    result_ = at(builder_, level_ + 1, IMPORT_KEYWORD);
    result_ = result_ && consumeToken(builder_, "import");
    result_ = result_ && IDENTIFIER_EXT(builder_, level_ + 1);
    result_ = result_ && importDirective_3(builder_, level_ + 1);
    result_ = result_ && importDirective_4(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, IMPORT_DIRECTIVE, result_, false, null);
    return result_;
  }

  // ("." "*" | "as" IDENTIFIER)?
  private static boolean importDirective_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importDirective_3")) return false;
    importDirective_3_0(builder_, level_ + 1);
    return true;
  }

  // "." "*" | "as" IDENTIFIER
  private static boolean importDirective_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importDirective_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = importDirective_3_0_0(builder_, level_ + 1);
    if (!result_) result_ = importDirective_3_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "." "*"
  private static boolean importDirective_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importDirective_3_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ".");
    result_ = result_ && consumeToken(builder_, "*");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "as" IDENTIFIER
  private static boolean importDirective_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importDirective_3_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "as");
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // SEMI?
  private static boolean importDirective_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importDirective_4")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // importDirective+
  public static boolean importList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<import list>");
    result_ = importDirective(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!importDirective(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "importList", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, level_, marker_, IMPORT_LIST, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "in" | "!in"
  public static boolean inOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<in operation>");
    result_ = consumeToken(builder_, "in");
    if (!result_) result_ = consumeToken(builder_, "!in");
    exit_section_(builder_, level_, marker_, IN_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE "[" DISABLE_NEWLINES expressionExt RESTORE_NEWLINES_STATE "]"
  public static boolean indices(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indices")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<indices>");
    result_ = indices_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "[");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && expressionExt(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "]");
    exit_section_(builder_, level_, marker_, INDICES, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean indices_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indices_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // rangeExpression infixFunctionCallPlus*
  static boolean infixFunctionCall(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "infixFunctionCall")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = rangeExpression(builder_, level_ + 1);
    result_ = result_ && infixFunctionCall_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // infixFunctionCallPlus*
  private static boolean infixFunctionCall_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "infixFunctionCall_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!infixFunctionCallPlus(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "infixFunctionCall_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE simpleNameOperation rangeExpression
  public static boolean infixFunctionCallPlus(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "infixFunctionCallPlus")) return false;
    if (!nextTokenIs(builder_, IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = infixFunctionCallPlus_0(builder_, level_ + 1);
    result_ = result_ && simpleNameOperation(builder_, level_ + 1);
    result_ = result_ && rangeExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, INFIX_FUNCTION_CALL_PLUS, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean infixFunctionCallPlus_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "infixFunctionCallPlus_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // annotationsPlus? "this" valueArguments
  //   | annotationsPlus? delegatorSuperCall
  static boolean initializer(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "initializer")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = initializer_0(builder_, level_ + 1);
    if (!result_) result_ = initializer_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus? "this" valueArguments
  private static boolean initializer_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "initializer_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = initializer_0_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "this");
    result_ = result_ && valueArguments(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean initializer_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "initializer_0_0")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  // annotationsPlus? delegatorSuperCall
  private static boolean initializer_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "initializer_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = initializer_1_0(builder_, level_ + 1);
    result_ = result_ && delegatorSuperCall(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean initializer_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "initializer_1_0")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // (initializer ",")* initializer
  public static boolean initializerExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "initializerExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<initializer ext>");
    result_ = initializerExt_0(builder_, level_ + 1);
    result_ = result_ && initializer(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, INITIALIZER_EXT, result_, false, null);
    return result_;
  }

  // (initializer ",")*
  private static boolean initializerExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "initializerExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!initializerExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "initializerExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // initializer ","
  private static boolean initializerExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "initializerExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = initializer(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "is" | "!is"
  public static boolean isOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "isOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<is operation>");
    result_ = consumeToken(builder_, "is");
    if (!result_) result_ = consumeToken(builder_, "!is");
    exit_section_(builder_, level_, marker_, IS_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // type
  static boolean isRHS(PsiBuilder builder_, int level_) {
    return type(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // jumpThrow
  //   | jumpReturn
  //   | jumpContinue
  //   | jumpBreak
  static boolean jump(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jump")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = jumpThrow(builder_, level_ + 1);
    if (!result_) result_ = jumpReturn(builder_, level_ + 1);
    if (!result_) result_ = jumpContinue(builder_, level_ + 1);
    if (!result_) result_ = jumpBreak(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "break" (!INTERRUPTED_WITH_NEWLINE label)?
  public static boolean jumpBreak(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpBreak")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<jump break>");
    result_ = consumeToken(builder_, "break");
    result_ = result_ && jumpBreak_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, JUMP_BREAK, result_, false, null);
    return result_;
  }

  // (!INTERRUPTED_WITH_NEWLINE label)?
  private static boolean jumpBreak_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpBreak_1")) return false;
    jumpBreak_1_0(builder_, level_ + 1);
    return true;
  }

  // !INTERRUPTED_WITH_NEWLINE label
  private static boolean jumpBreak_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpBreak_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = jumpBreak_1_0_0(builder_, level_ + 1);
    result_ = result_ && label(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean jumpBreak_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpBreak_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "continue" (!INTERRUPTED_WITH_NEWLINE label)?
  public static boolean jumpContinue(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpContinue")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<jump continue>");
    result_ = consumeToken(builder_, "continue");
    result_ = result_ && jumpContinue_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, JUMP_CONTINUE, result_, false, null);
    return result_;
  }

  // (!INTERRUPTED_WITH_NEWLINE label)?
  private static boolean jumpContinue_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpContinue_1")) return false;
    jumpContinue_1_0(builder_, level_ + 1);
    return true;
  }

  // !INTERRUPTED_WITH_NEWLINE label
  private static boolean jumpContinue_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpContinue_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = jumpContinue_1_0_0(builder_, level_ + 1);
    result_ = result_ && label(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean jumpContinue_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpContinue_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "return" (!INTERRUPTED_WITH_NEWLINE label)? (!INTERRUPTED_WITH_NEWLINE expression)?
  public static boolean jumpReturn(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpReturn")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<jump return>");
    result_ = consumeToken(builder_, "return");
    result_ = result_ && jumpReturn_1(builder_, level_ + 1);
    result_ = result_ && jumpReturn_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, JUMP_RETURN, result_, false, null);
    return result_;
  }

  // (!INTERRUPTED_WITH_NEWLINE label)?
  private static boolean jumpReturn_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpReturn_1")) return false;
    jumpReturn_1_0(builder_, level_ + 1);
    return true;
  }

  // !INTERRUPTED_WITH_NEWLINE label
  private static boolean jumpReturn_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpReturn_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = jumpReturn_1_0_0(builder_, level_ + 1);
    result_ = result_ && label(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean jumpReturn_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpReturn_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // (!INTERRUPTED_WITH_NEWLINE expression)?
  private static boolean jumpReturn_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpReturn_2")) return false;
    jumpReturn_2_0(builder_, level_ + 1);
    return true;
  }

  // !INTERRUPTED_WITH_NEWLINE expression
  private static boolean jumpReturn_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpReturn_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = jumpReturn_2_0_0(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean jumpReturn_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpReturn_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "throw" expression
  public static boolean jumpThrow(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jumpThrow")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<jump throw>");
    result_ = consumeToken(builder_, "throw");
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, JUMP_THROW, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // preamble (toplevelObject)*
  static boolean kotlinFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "kotlinFile")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = preamble(builder_, level_ + 1);
    result_ = result_ && kotlinFile_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (toplevelObject)*
  private static boolean kotlinFile_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "kotlinFile_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!kotlinFile_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "kotlinFile_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // (toplevelObject)
  private static boolean kotlinFile_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "kotlinFile_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = toplevelObject(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // labelLabel
  public static boolean label(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "label")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<label>");
    result_ = labelLabel(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LABEL, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // DISABLE_JOINING_COMPLEX_TOKENS
  //       label
  //       RESTORE_JOINING_COMPLEX_TOKENS_STATE prefixUnaryExpression
  public static boolean labelExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labelExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<label expression>");
    result_ = disableJoiningComplexTokens(builder_, level_ + 1, marker_);
    result_ = result_ && label(builder_, level_ + 1);
    result_ = result_ && restoreJoiningComplexTokensState(builder_, level_ + 1);
    result_ = result_ && prefixUnaryExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LABEL_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "@"
  //   | "@@"
  //   | LABEL_IDENTIFIER
  public static boolean labelLabel(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labelLabel")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<label label>");
    result_ = consumeToken(builder_, "@");
    if (!result_) result_ = consumeToken(builder_, "@@");
    if (!result_) result_ = consumeToken(builder_, LABEL_IDENTIFIER);
    exit_section_(builder_, level_, marker_, LABEL_LABEL, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // valueParameterList "->" type
  public static boolean leftFunctionType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "leftFunctionType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<left function type>");
    result_ = valueParameterList(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "->");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LEFT_FUNCTION_TYPE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // binaryConstant
  //   | stringTemplate
  //   //| NoEscapeString
  //   | INTEGER_CONSTANT
  //   | CHARACTER_CONSTANT
  //   | FLOAT_CONSTANT
  //   | NULL
  static boolean literalConstant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "literalConstant")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = binaryConstant(builder_, level_ + 1);
    if (!result_) result_ = stringTemplate(builder_, level_ + 1);
    if (!result_) result_ = INTEGER_CONSTANT(builder_, level_ + 1);
    if (!result_) result_ = CHARACTER_CONSTANT(builder_, level_ + 1);
    if (!result_) result_ = FLOAT_CONSTANT(builder_, level_ + 1);
    if (!result_) result_ = NULL(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // REGULAR_STRING_PART
  public static boolean literalStringTemplateEntry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "literalStringTemplateEntry")) return false;
    if (!nextTokenIs(builder_, REGULAR_STRING_PART)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, REGULAR_STRING_PART);
    exit_section_(builder_, marker_, LITERAL_STRING_TEMPLATE_ENTRY, result_);
    return result_;
  }

  /* ********************************************************** */
  // "["  FORBID_SHORT_ANNOTATIONS DISABLE_NEWLINES annotationEntry+ RESTORE_NEWLINES_STATE RESTORE_ANNOTATIONS_STATE "]"
  public static boolean longAnnotation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "longAnnotation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<long annotation>");
    result_ = consumeToken(builder_, "[");
    result_ = result_ && forbidShortAnnotations(builder_, level_ + 1, marker_);
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && longAnnotation_3(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && restoreAnnotationsState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "]");
    exit_section_(builder_, level_, marker_, LONG_ANNOTATION, result_, false, null);
    return result_;
  }

  // annotationEntry+
  private static boolean longAnnotation_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "longAnnotation_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = annotationEntry(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!annotationEntry(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "longAnnotation_3", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // LONG_TEMPLATE_ENTRY_START expression LONG_TEMPLATE_ENTRY_END
  public static boolean longTemplate(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "longTemplate")) return false;
    if (!nextTokenIs(builder_, LONG_TEMPLATE_ENTRY_START)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LONG_TEMPLATE_ENTRY_START);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, LONG_TEMPLATE_ENTRY_END);
    exit_section_(builder_, marker_, LONG_TEMPLATE, result_);
    return result_;
  }

  /* ********************************************************** */
  // forLoop
  //   | whileLoop
  //   | doWhileLoop
  static boolean loop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "loop")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = forLoop(builder_, level_ + 1);
    if (!result_) result_ = whileLoop(builder_, level_ + 1);
    if (!result_) result_ = doWhileLoop(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // expression
  public static boolean loopRange(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "loopRange")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<loop range>");
    result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LOOP_RANGE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // classObject
  //   | object
  //   | function
  //   | property
  //   | classDeclaration
  //   | typedef
  //   | anonymousInitializer
  static boolean memberDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "memberDeclaration")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = classObject(builder_, level_ + 1);
    if (!result_) result_ = object(builder_, level_ + 1);
    if (!result_) result_ = function(builder_, level_ + 1);
    if (!result_) result_ = property(builder_, level_ + 1);
    if (!result_) result_ = classDeclaration(builder_, level_ + 1);
    if (!result_) result_ = typedef(builder_, level_ + 1);
    if (!result_) result_ = anonymousInitializer(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<at OVERRIDE_KEYWORD>> "override"
  //   | <<at OPEN_KEYWORD>> "open"
  //   | <<at FINAL_KEYWORD>> "final"
  //   | <<at ABSTRACT_KEYWORD>> "abstract"
  static boolean memberModifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "memberModifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = memberModifier_0(builder_, level_ + 1);
    if (!result_) result_ = memberModifier_1(builder_, level_ + 1);
    if (!result_) result_ = memberModifier_2(builder_, level_ + 1);
    if (!result_) result_ = memberModifier_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at OVERRIDE_KEYWORD>> "override"
  private static boolean memberModifier_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "memberModifier_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, OVERRIDE_KEYWORD);
    result_ = result_ && consumeToken(builder_, "override");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at OPEN_KEYWORD>> "open"
  private static boolean memberModifier_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "memberModifier_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, OPEN_KEYWORD);
    result_ = result_ && consumeToken(builder_, "open");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at FINAL_KEYWORD>> "final"
  private static boolean memberModifier_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "memberModifier_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, FINAL_KEYWORD);
    result_ = result_ && consumeToken(builder_, "final");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at ABSTRACT_KEYWORD>> "abstract"
  private static boolean memberModifier_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "memberModifier_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, ABSTRACT_KEYWORD);
    result_ = result_ && consumeToken(builder_, "abstract");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // classModifier
  //   | accessModifier
  //   | varianceAnnotation
  //   | memberModifier
  //   //| parameterKind
  //   | annotation
  static boolean modifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = classModifier(builder_, level_ + 1);
    if (!result_) result_ = accessModifier(builder_, level_ + 1);
    if (!result_) result_ = varianceAnnotation(builder_, level_ + 1);
    if (!result_) result_ = memberModifier(builder_, level_ + 1);
    if (!result_) result_ = annotation(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // modifier+
  public static boolean modifierList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifierList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<modifier list>");
    result_ = modifier(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!modifier(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "modifierList", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, level_, marker_, MODIFIER_LIST, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // ALLOW_SHORT_ANNOTATIONS modifierList FORBID_SHORT_ANNOTATIONS
  static boolean modifierListWithShortAnnotations(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifierListWithShortAnnotations")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = allowShortAnnotations(builder_, level_ + 1, marker_);
    result_ = result_ && modifierList(builder_, level_ + 1);
    result_ = result_ && forbidShortAnnotations(builder_, level_ + 1, marker_);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (valueParameterNoType ",")* valueParameterNoType
  static boolean modifiersIDENTIFIERExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifiersIDENTIFIERExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = modifiersIDENTIFIERExt_0(builder_, level_ + 1);
    result_ = result_ && valueParameterNoType(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (valueParameterNoType ",")*
  private static boolean modifiersIDENTIFIERExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifiersIDENTIFIERExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!modifiersIDENTIFIERExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "modifiersIDENTIFIERExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // valueParameterNoType ","
  private static boolean modifiersIDENTIFIERExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifiersIDENTIFIERExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = valueParameterNoType(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (valueParameterWithType ",")* valueParameterWithType
  static boolean modifiersIDENTIFIERTypeExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifiersIDENTIFIERTypeExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = modifiersIDENTIFIERTypeExt_0(builder_, level_ + 1);
    result_ = result_ && valueParameterWithType(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (valueParameterWithType ",")*
  private static boolean modifiersIDENTIFIERTypeExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifiersIDENTIFIERTypeExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!modifiersIDENTIFIERTypeExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "modifiersIDENTIFIERTypeExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // valueParameterWithType ","
  private static boolean modifiersIDENTIFIERTypeExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "modifiersIDENTIFIERTypeExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = valueParameterWithType(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "(" DISABLE_NEWLINES variableDeclarationEntryExt RESTORE_NEWLINES_STATE ")"
  public static boolean multipleVariableDeclarations(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multipleVariableDeclarations")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<multiple variable declarations>");
    result_ = consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && variableDeclarationEntryExt(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, level_, marker_, MULTIPLE_VARIABLE_DECLARATIONS, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // typeRHS multiplicativeExpressionPlus*
  static boolean multiplicativeExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeRHS(builder_, level_ + 1);
    result_ = result_ && multiplicativeExpression_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // multiplicativeExpressionPlus*
  private static boolean multiplicativeExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!multiplicativeExpressionPlus(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "multiplicativeExpression_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE multiplicativeOperation typeRHS
  public static boolean multiplicativeExpressionPlus(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpressionPlus")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<multiplicative expression plus>");
    result_ = multiplicativeExpressionPlus_0(builder_, level_ + 1);
    result_ = result_ && multiplicativeOperation(builder_, level_ + 1);
    result_ = result_ && typeRHS(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, MULTIPLICATIVE_EXPRESSION_PLUS, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean multiplicativeExpressionPlus_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpressionPlus_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "*" | "/" | "%"
  public static boolean multiplicativeOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<multiplicative operation>");
    result_ = consumeToken(builder_, "*");
    if (!result_) result_ = consumeToken(builder_, "/");
    if (!result_) result_ = consumeToken(builder_, "%");
    exit_section_(builder_, level_, marker_, MULTIPLICATIVE_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // namedInfixFirst
  //   | elvisExpression namedInfixPlus*
  static boolean namedInfix(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namedInfix")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = namedInfixFirst(builder_, level_ + 1);
    if (!result_) result_ = namedInfix_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // elvisExpression namedInfixPlus*
  private static boolean namedInfix_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namedInfix_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = elvisExpression(builder_, level_ + 1);
    result_ = result_ && namedInfix_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // namedInfixPlus*
  private static boolean namedInfix_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namedInfix_1_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!namedInfixPlus(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "namedInfix_1_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // elvisExpression !INTERRUPTED_WITH_NEWLINE isOperation isRHS
  public static boolean namedInfixFirst(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namedInfixFirst")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<named infix first>");
    result_ = elvisExpression(builder_, level_ + 1);
    result_ = result_ && namedInfixFirst_1(builder_, level_ + 1);
    result_ = result_ && isOperation(builder_, level_ + 1);
    result_ = result_ && isRHS(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, NAMED_INFIX_FIRST, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean namedInfixFirst_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namedInfixFirst_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE inOperation elvisExpression
  public static boolean namedInfixPlus(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namedInfixPlus")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<named infix plus>");
    result_ = namedInfixPlus_0(builder_, level_ + 1);
    result_ = result_ && inOperation(builder_, level_ + 1);
    result_ = result_ && elvisExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, NAMED_INFIX_PLUS, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean namedInfixPlus_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namedInfixPlus_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "?"
  public static boolean nullableType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "nullableType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<nullable type>");
    result_ = consumeToken(builder_, "?");
    exit_section_(builder_, level_, marker_, NULLABLE_TYPE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // modifierListWithShortAnnotations? "object" objectName (":" delegationSpecifierExt)? classBody?
  public static boolean object(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<object>");
    result_ = object_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "object");
    result_ = result_ && objectName(builder_, level_ + 1);
    result_ = result_ && object_3(builder_, level_ + 1);
    result_ = result_ && object_4(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, OBJECT, result_, false, null);
    return result_;
  }

  // modifierListWithShortAnnotations?
  private static boolean object_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_0")) return false;
    modifierListWithShortAnnotations(builder_, level_ + 1);
    return true;
  }

  // (":" delegationSpecifierExt)?
  private static boolean object_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_3")) return false;
    object_3_0(builder_, level_ + 1);
    return true;
  }

  // ":" delegationSpecifierExt
  private static boolean object_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && delegationSpecifierExt(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // classBody?
  private static boolean object_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_4")) return false;
    classBody(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "object" (":" delegationSpecifierExt)? classBody?
  public static boolean objectDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectDeclaration")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<object declaration>");
    result_ = consumeToken(builder_, "object");
    result_ = result_ && objectDeclaration_1(builder_, level_ + 1);
    result_ = result_ && objectDeclaration_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, OBJECT_DECLARATION, result_, false, null);
    return result_;
  }

  // (":" delegationSpecifierExt)?
  private static boolean objectDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectDeclaration_1")) return false;
    objectDeclaration_1_0(builder_, level_ + 1);
    return true;
  }

  // ":" delegationSpecifierExt
  private static boolean objectDeclaration_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectDeclaration_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && delegationSpecifierExt(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // classBody?
  private static boolean objectDeclaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectDeclaration_2")) return false;
    classBody(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean objectDeclarationName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectDeclarationName")) return false;
    if (!nextTokenIs(builder_, IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IDENTIFIER);
    exit_section_(builder_, marker_, OBJECT_DECLARATION_NAME, result_);
    return result_;
  }

  /* ********************************************************** */
  // objectDeclaration
  public static boolean objectLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteral")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<object literal>");
    result_ = objectDeclaration(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, OBJECT_LITERAL, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean objectName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectName")) return false;
    if (!nextTokenIs(builder_, IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IDENTIFIER);
    exit_section_(builder_, marker_, OBJECT_NAME, result_);
    return result_;
  }

  /* ********************************************************** */
  // "object" (":" delegationSpecifierExt)? classBody?
  public static boolean objectUnnamed(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectUnnamed")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<object unnamed>");
    result_ = consumeToken(builder_, "object");
    result_ = result_ && objectUnnamed_1(builder_, level_ + 1);
    result_ = result_ && objectUnnamed_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, OBJECT_UNNAMED, result_, false, null);
    return result_;
  }

  // (":" delegationSpecifierExt)?
  private static boolean objectUnnamed_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectUnnamed_1")) return false;
    objectUnnamed_1_0(builder_, level_ + 1);
    return true;
  }

  // ":" delegationSpecifierExt
  private static boolean objectUnnamed_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectUnnamed_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && delegationSpecifierExt(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // classBody?
  private static boolean objectUnnamed_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectUnnamed_2")) return false;
    classBody(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // ((/*optionalProjection*/ typeProjection | asterisk) ",")* (/*optionalProjection*/ typeProjection | asterisk)
  static boolean optionalProjectionTypeExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "optionalProjectionTypeExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = optionalProjectionTypeExt_0(builder_, level_ + 1);
    result_ = result_ && optionalProjectionTypeExt_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ((/*optionalProjection*/ typeProjection | asterisk) ",")*
  private static boolean optionalProjectionTypeExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "optionalProjectionTypeExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!optionalProjectionTypeExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "optionalProjectionTypeExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // (/*optionalProjection*/ typeProjection | asterisk) ","
  private static boolean optionalProjectionTypeExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "optionalProjectionTypeExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = optionalProjectionTypeExt_0_0_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeProjection | asterisk
  private static boolean optionalProjectionTypeExt_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "optionalProjectionTypeExt_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeProjection(builder_, level_ + 1);
    if (!result_) result_ = asterisk(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeProjection | asterisk
  private static boolean optionalProjectionTypeExt_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "optionalProjectionTypeExt_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeProjection(builder_, level_ + 1);
    if (!result_) result_ = asterisk(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "package" IDENTIFIER_EXT "{"
  //        importDirective*
  //        toplevelObject*
  //     "}"
  public static boolean packageDirective(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageDirective")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<package directive>");
    result_ = consumeToken(builder_, "package");
    result_ = result_ && IDENTIFIER_EXT(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "{");
    result_ = result_ && packageDirective_3(builder_, level_ + 1);
    result_ = result_ && packageDirective_4(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "}");
    exit_section_(builder_, level_, marker_, PACKAGE_DIRECTIVE, result_, false, null);
    return result_;
  }

  // importDirective*
  private static boolean packageDirective_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageDirective_3")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!importDirective(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "packageDirective_3", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // toplevelObject*
  private static boolean packageDirective_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageDirective_4")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!toplevelObject(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "packageDirective_4", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // (ALLOW_SHORT_ANNOTATIONS modifierList? RESTORE_ANNOTATIONS_STATE
  //   "package" IDENTIFIER_EXT SEMI?)?
  public static boolean packageHeader(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageHeader")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<package header>");
    packageHeader_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PACKAGE_HEADER, true, false, null);
    return true;
  }

  // ALLOW_SHORT_ANNOTATIONS modifierList? RESTORE_ANNOTATIONS_STATE
  //   "package" IDENTIFIER_EXT SEMI?
  private static boolean packageHeader_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageHeader_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = allowShortAnnotations(builder_, level_ + 1, marker_);
    result_ = result_ && packageHeader_0_1(builder_, level_ + 1);
    result_ = result_ && restoreAnnotationsState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "package");
    result_ = result_ && IDENTIFIER_EXT(builder_, level_ + 1);
    result_ = result_ && packageHeader_0_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // modifierList?
  private static boolean packageHeader_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageHeader_0_1")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // SEMI?
  private static boolean packageHeader_0_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageHeader_0_5")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER (":" type)?
  public static boolean parameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter")) return false;
    if (!nextTokenIs(builder_, IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IDENTIFIER);
    result_ = result_ && parameter_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, PARAMETER, result_);
    return result_;
  }

  // (":" type)?
  private static boolean parameter_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter_1")) return false;
    parameter_1_0(builder_, level_ + 1);
    return true;
  }

  // ":" type
  private static boolean parameter_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "lazy"
  //   | "out"
  //   | "ref"
  static boolean parameterKind(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterKind")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "lazy");
    if (!result_) result_ = consumeToken(builder_, "out");
    if (!result_) result_ = consumeToken(builder_, "ref");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (parameterOrModifierType ",")* parameterOrModifierType
  static boolean parameterModifiersTypeExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterModifiersTypeExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parameterModifiersTypeExt_0(builder_, level_ + 1);
    result_ = result_ && parameterOrModifierType(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (parameterOrModifierType ",")*
  private static boolean parameterModifiersTypeExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterModifiersTypeExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!parameterModifiersTypeExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "parameterModifiersTypeExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // parameterOrModifierType ","
  private static boolean parameterModifiersTypeExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterModifiersTypeExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parameterOrModifierType(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // privateParameterReference |  modifierList?  type
  public static boolean parameterOrModifierType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterOrModifierType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<parameter or modifier type>");
    result_ = privateParameterReference(builder_, level_ + 1);
    if (!result_) result_ = parameterOrModifierType_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PARAMETER_OR_MODIFIER_TYPE, result_, false, null);
    return result_;
  }

  // modifierList?  type
  private static boolean parameterOrModifierType_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterOrModifierType_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parameterOrModifierType_1_0(builder_, level_ + 1);
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // modifierList?
  private static boolean parameterOrModifierType_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterOrModifierType_1_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "(" DISABLE_NEWLINES expression RESTORE_NEWLINES_STATE ")"
  public static boolean parenthesizedExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<parenthesized expression>");
    result_ = consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, level_, marker_, PARENTHESIZED_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // plusPlusAndOthersOperation
  public static boolean plusPlusAndOthersExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "plusPlusAndOthersExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<plus plus and others expression>");
    result_ = plusPlusAndOthersOperation(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PLUS_PLUS_AND_OTHERS_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "++" | "--" | "!!"
  public static boolean plusPlusAndOthersOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "plusPlusAndOthersOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<plus plus and others operation>");
    result_ = consumeToken(builder_, "++");
    if (!result_) result_ = consumeToken(builder_, "--");
    if (!result_) result_ = consumeToken(builder_, "!!");
    exit_section_(builder_, level_, marker_, PLUS_PLUS_AND_OTHERS_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // postfixUnaryExpression2 dotQualifiedExpression*
  static boolean postfixUnaryExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfixUnaryExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = postfixUnaryExpression2(builder_, level_ + 1);
    result_ = result_ && postfixUnaryExpression_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // dotQualifiedExpression*
  private static boolean postfixUnaryExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfixUnaryExpression_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!dotQualifiedExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "postfixUnaryExpression_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // atomicOrCallable postfixUnaryOperation*
  static boolean postfixUnaryExpression2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfixUnaryExpression2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = atomicOrCallable(builder_, level_ + 1);
    result_ = result_ && postfixUnaryExpression2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // postfixUnaryOperation*
  private static boolean postfixUnaryExpression2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfixUnaryExpression2_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!postfixUnaryOperation(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "postfixUnaryExpression2_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE (
  //     plusPlusAndOthersExpression
  //   | callSuffix
  //   | arrayAccess
  //   | safeAccessExpression
  //   | elvisAccessExpression
  //   )
  static boolean postfixUnaryOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfixUnaryOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = postfixUnaryOperation_0(builder_, level_ + 1);
    result_ = result_ && postfixUnaryOperation_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean postfixUnaryOperation_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfixUnaryOperation_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // plusPlusAndOthersExpression
  //   | callSuffix
  //   | arrayAccess
  //   | safeAccessExpression
  //   | elvisAccessExpression
  private static boolean postfixUnaryOperation_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "postfixUnaryOperation_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = plusPlusAndOthersExpression(builder_, level_ + 1);
    if (!result_) result_ = callSuffix(builder_, level_ + 1);
    if (!result_) result_ = arrayAccess(builder_, level_ + 1);
    if (!result_) result_ = safeAccessExpression(builder_, level_ + 1);
    if (!result_) result_ = elvisAccessExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // packageHeader importList?
  static boolean preamble(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "preamble")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = packageHeader(builder_, level_ + 1);
    result_ = result_ && preamble_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // importList?
  private static boolean preamble_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "preamble_1")) return false;
    importList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "-" | "+"
  //   | "++" | "--"
  //   | "!"
  public static boolean prefixOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<prefix operation>");
    result_ = consumeToken(builder_, "-");
    if (!result_) result_ = consumeToken(builder_, "+");
    if (!result_) result_ = consumeToken(builder_, "++");
    if (!result_) result_ = consumeToken(builder_, "--");
    if (!result_) result_ = consumeToken(builder_, "!");
    exit_section_(builder_, level_, marker_, PREFIX_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // DISABLE_JOINING_COMPLEX_TOKENS
  //       prefixOperation
  //       RESTORE_JOINING_COMPLEX_TOKENS_STATE prefixUnaryExpression
  public static boolean prefixOperationExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixOperationExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<prefix operation expression>");
    result_ = disableJoiningComplexTokens(builder_, level_ + 1, marker_);
    result_ = result_ && prefixOperation(builder_, level_ + 1);
    result_ = result_ && restoreJoiningComplexTokensState(builder_, level_ + 1);
    result_ = result_ && prefixUnaryExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PREFIX_OPERATION_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // postfixUnaryExpression
  //   | prefixUnaryOperation
  //   | annotatedExpression
  static boolean prefixUnaryExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixUnaryExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = postfixUnaryExpression(builder_, level_ + 1);
    if (!result_) result_ = prefixUnaryOperation(builder_, level_ + 1);
    if (!result_) result_ = annotatedExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // prefixOperationExpression | labelExpression
  static boolean prefixUnaryOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixUnaryOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = prefixOperationExpression(builder_, level_ + 1);
    if (!result_) result_ = labelExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // modifier+
  public static boolean primaryConstructorModifierList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primaryConstructorModifierList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<primary constructor modifier list>");
    result_ = modifier(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!modifier(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "primaryConstructorModifierList", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, level_, marker_, PRIMARY_CONSTRUCTOR_MODIFIER_LIST, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // IDENTIFIER_NEXT IDENTIFIER (":" type)?
  static boolean privateParameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "privateParameter")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifierNext(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    result_ = result_ && privateParameter_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (":" type)?
  private static boolean privateParameter_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "privateParameter_2")) return false;
    privateParameter_2_0(builder_, level_ + 1);
    return true;
  }

  // ":" type
  private static boolean privateParameter_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "privateParameter_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // IDENTIFIER (":" type)
  //   | userTypeReference
  static boolean privateParameterReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "privateParameterReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = privateParameterReference_0(builder_, level_ + 1);
    if (!result_) result_ = userTypeReference(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // IDENTIFIER (":" type)
  private static boolean privateParameterReference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "privateParameterReference_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IDENTIFIER);
    result_ = result_ && privateParameterReference_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ":" type
  private static boolean privateParameterReference_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "privateParameterReference_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // referenceExpression typeArgumentList?
  static boolean privateSimpleUserType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "privateSimpleUserType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && privateSimpleUserType_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeArgumentList?
  private static boolean privateSimpleUserType_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "privateSimpleUserType_1")) return false;
    typeArgumentList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // modifierListWithShortAnnotations? ("val" | "var")
  //       typeParameters? DISABLE_JOINING_COMPLEX_TOKENS
  //       (<<stopAtLastDot marker_>> type <<unStop>> "." | annotationsPlus?)?
  //       (multipleVariableDeclarations | variableDeclarationEntry) RESTORE_JOINING_COMPLEX_TOKENS_STATE
  //       typeConstraints?
  //       //("by" (getter setter | setter getter | setter | getter?)
  //       //| "=" expression /*SEMI?*/ (getter setter | setter getter | setter | getter)
  //       //| "=" expression)
  //       ((propertyDelegate | "=" expression) SEMI?)?
  //       //was: (getter? setter? | setter? getter?) SEMI?
  //       (getter setter | setter getter | setter | getter?)
  public static boolean property(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<property>");
    result_ = property_0(builder_, level_ + 1);
    result_ = result_ && property_1(builder_, level_ + 1);
    result_ = result_ && property_2(builder_, level_ + 1);
    result_ = result_ && disableJoiningComplexTokens(builder_, level_ + 1, marker_);
    result_ = result_ && property_4(builder_, level_ + 1);
    result_ = result_ && property_5(builder_, level_ + 1);
    result_ = result_ && restoreJoiningComplexTokensState(builder_, level_ + 1);
    result_ = result_ && property_7(builder_, level_ + 1);
    result_ = result_ && property_8(builder_, level_ + 1);
    result_ = result_ && property_9(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PROPERTY, result_, false, null);
    return result_;
  }

  // modifierListWithShortAnnotations?
  private static boolean property_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_0")) return false;
    modifierListWithShortAnnotations(builder_, level_ + 1);
    return true;
  }

  // "val" | "var"
  private static boolean property_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "val");
    if (!result_) result_ = consumeToken(builder_, "var");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeParameters?
  private static boolean property_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_2")) return false;
    typeParameters(builder_, level_ + 1);
    return true;
  }

  // (<<stopAtLastDot marker_>> type <<unStop>> "." | annotationsPlus?)?
  private static boolean property_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_4")) return false;
    property_4_0(builder_, level_ + 1);
    return true;
  }

  // <<stopAtLastDot marker_>> type <<unStop>> "." | annotationsPlus?
  private static boolean property_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = property_4_0_0(builder_, level_ + 1);
    if (!result_) result_ = property_4_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<stopAtLastDot marker_>> type <<unStop>> "."
  private static boolean property_4_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_4_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = stopAtLastDot(builder_, level_ + 1, marker_);
    result_ = result_ && type(builder_, level_ + 1);
    result_ = result_ && unStop(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ".");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean property_4_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_4_0_1")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  // multipleVariableDeclarations | variableDeclarationEntry
  private static boolean property_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_5")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = multipleVariableDeclarations(builder_, level_ + 1);
    if (!result_) result_ = variableDeclarationEntry(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeConstraints?
  private static boolean property_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_7")) return false;
    typeConstraints(builder_, level_ + 1);
    return true;
  }

  // ((propertyDelegate | "=" expression) SEMI?)?
  private static boolean property_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_8")) return false;
    property_8_0(builder_, level_ + 1);
    return true;
  }

  // (propertyDelegate | "=" expression) SEMI?
  private static boolean property_8_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_8_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = property_8_0_0(builder_, level_ + 1);
    result_ = result_ && property_8_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // propertyDelegate | "=" expression
  private static boolean property_8_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_8_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = propertyDelegate(builder_, level_ + 1);
    if (!result_) result_ = property_8_0_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "=" expression
  private static boolean property_8_0_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_8_0_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "=");
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // SEMI?
  private static boolean property_8_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_8_0_1")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  // getter setter | setter getter | setter | getter?
  private static boolean property_9(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_9")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = property_9_0(builder_, level_ + 1);
    if (!result_) result_ = property_9_1(builder_, level_ + 1);
    if (!result_) result_ = setter(builder_, level_ + 1);
    if (!result_) result_ = property_9_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // getter setter
  private static boolean property_9_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_9_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = getter(builder_, level_ + 1);
    result_ = result_ && setter(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // setter getter
  private static boolean property_9_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_9_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = setter(builder_, level_ + 1);
    result_ = result_ && getter(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // getter?
  private static boolean property_9_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_9_3")) return false;
    getter(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // <<at BY_KEYWORD>> "by" expression
  public static boolean propertyDelegate(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyDelegate")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<property delegate>");
    result_ = at(builder_, level_ + 1, BY_KEYWORD);
    result_ = result_ && consumeToken(builder_, "by");
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PROPERTY_DELEGATE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // modifierList? ("val" | "var")
  //       typeParameters? DISABLE_JOINING_COMPLEX_TOKENS
  //       (<<stopAtLastDot marker_>> type <<unStop>> "." | annotationsPlus?)?
  //       (multipleVariableDeclarations | variableDeclarationEntry) RESTORE_JOINING_COMPLEX_TOKENS_STATE
  //       typeConstraints?
  //       (propertyDelegate | "=" expression)?
  public static boolean propertyLocal(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<property local>");
    result_ = propertyLocal_0(builder_, level_ + 1);
    result_ = result_ && propertyLocal_1(builder_, level_ + 1);
    result_ = result_ && propertyLocal_2(builder_, level_ + 1);
    result_ = result_ && disableJoiningComplexTokens(builder_, level_ + 1, marker_);
    result_ = result_ && propertyLocal_4(builder_, level_ + 1);
    result_ = result_ && propertyLocal_5(builder_, level_ + 1);
    result_ = result_ && restoreJoiningComplexTokensState(builder_, level_ + 1);
    result_ = result_ && propertyLocal_7(builder_, level_ + 1);
    result_ = result_ && propertyLocal_8(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PROPERTY_LOCAL, result_, false, null);
    return result_;
  }

  // modifierList?
  private static boolean propertyLocal_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // "val" | "var"
  private static boolean propertyLocal_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "val");
    if (!result_) result_ = consumeToken(builder_, "var");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeParameters?
  private static boolean propertyLocal_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_2")) return false;
    typeParameters(builder_, level_ + 1);
    return true;
  }

  // (<<stopAtLastDot marker_>> type <<unStop>> "." | annotationsPlus?)?
  private static boolean propertyLocal_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_4")) return false;
    propertyLocal_4_0(builder_, level_ + 1);
    return true;
  }

  // <<stopAtLastDot marker_>> type <<unStop>> "." | annotationsPlus?
  private static boolean propertyLocal_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = propertyLocal_4_0_0(builder_, level_ + 1);
    if (!result_) result_ = propertyLocal_4_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<stopAtLastDot marker_>> type <<unStop>> "."
  private static boolean propertyLocal_4_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_4_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = stopAtLastDot(builder_, level_ + 1, marker_);
    result_ = result_ && type(builder_, level_ + 1);
    result_ = result_ && unStop(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ".");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean propertyLocal_4_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_4_0_1")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  // multipleVariableDeclarations | variableDeclarationEntry
  private static boolean propertyLocal_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_5")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = multipleVariableDeclarations(builder_, level_ + 1);
    if (!result_) result_ = variableDeclarationEntry(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeConstraints?
  private static boolean propertyLocal_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_7")) return false;
    typeConstraints(builder_, level_ + 1);
    return true;
  }

  // (propertyDelegate | "=" expression)?
  private static boolean propertyLocal_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_8")) return false;
    propertyLocal_8_0(builder_, level_ + 1);
    return true;
  }

  // propertyDelegate | "=" expression
  private static boolean propertyLocal_8_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_8_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = propertyDelegate(builder_, level_ + 1);
    if (!result_) result_ = propertyLocal_8_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "=" expression
  private static boolean propertyLocal_8_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLocal_8_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "=");
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // additiveExpression rangeExpressionPlus*
  static boolean rangeExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = additiveExpression(builder_, level_ + 1);
    result_ = result_ && rangeExpression_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // rangeExpressionPlus*
  private static boolean rangeExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeExpression_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!rangeExpressionPlus(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "rangeExpression_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !INTERRUPTED_WITH_NEWLINE rangeOperation additiveExpression
  public static boolean rangeExpressionPlus(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeExpressionPlus")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<range expression plus>");
    result_ = rangeExpressionPlus_0(builder_, level_ + 1);
    result_ = result_ && rangeOperation(builder_, level_ + 1);
    result_ = result_ && additiveExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, RANGE_EXPRESSION_PLUS, result_, false, null);
    return result_;
  }

  // !INTERRUPTED_WITH_NEWLINE
  private static boolean rangeExpressionPlus_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeExpressionPlus_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interruptedWithNewLine(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // ".."
  public static boolean rangeOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<range operation>");
    result_ = consumeToken(builder_, "..");
    exit_section_(builder_, level_, marker_, RANGE_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // IDENTIFIER_NEXT IDENTIFIER
  public static boolean referenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<reference expression>");
    result_ = identifierNext(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    exit_section_(builder_, level_, marker_, REFERENCE_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // kotlinFile
  static boolean root(PsiBuilder builder_, int level_) {
    return kotlinFile(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // SAFE_ACCESS (atomicExpression | callableReference)
  public static boolean safeAccessExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "safeAccessExpression")) return false;
    if (!nextTokenIs(builder_, SAFE_ACCESS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, SAFE_ACCESS);
    result_ = result_ && safeAccessExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SAFE_ACCESS_EXPRESSION, result_, false, null);
    return result_;
  }

  // atomicExpression | callableReference
  private static boolean safeAccessExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "safeAccessExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = atomicExpression(builder_, level_ + 1);
    if (!result_) result_ = callableReference(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // statementsBlock
  public static boolean script(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "script")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<script>");
    result_ = statementsBlock(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SCRIPT, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // preamble script
  static boolean scriptFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptFile")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = preamble(builder_, level_ + 1);
    result_ = result_ && script(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "This"
  public static boolean selfType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "selfType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<self type>");
    result_ = consumeToken(builder_, "This");
    exit_section_(builder_, level_, marker_, SELF_TYPE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // modifierList? <<at SET_KEYWORD>> "set" "(" DISABLE_NEWLINES modifierList? singleValueParameterList RESTORE_NEWLINES_STATE")" functionBody
  //   | modifierList? <<at SET_KEYWORD>> "set"
  public static boolean setter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setter")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<setter>");
    result_ = setter_0(builder_, level_ + 1);
    if (!result_) result_ = setter_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SETTER, result_, false, null);
    return result_;
  }

  // modifierList? <<at SET_KEYWORD>> "set" "(" DISABLE_NEWLINES modifierList? singleValueParameterList RESTORE_NEWLINES_STATE")" functionBody
  private static boolean setter_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setter_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = setter_0_0(builder_, level_ + 1);
    result_ = result_ && at(builder_, level_ + 1, SET_KEYWORD);
    result_ = result_ && consumeToken(builder_, "set");
    result_ = result_ && consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && setter_0_5(builder_, level_ + 1);
    result_ = result_ && singleValueParameterList(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    result_ = result_ && functionBody(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // modifierList?
  private static boolean setter_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setter_0_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // modifierList?
  private static boolean setter_0_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setter_0_5")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // modifierList? <<at SET_KEYWORD>> "set"
  private static boolean setter_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setter_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = setter_1_0(builder_, level_ + 1);
    result_ = result_ && at(builder_, level_ + 1, SET_KEYWORD);
    result_ = result_ && consumeToken(builder_, "set");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // modifierList?
  private static boolean setter_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setter_1_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // SHORT_TEMPLATE_ENTRY_START (referenceExpression | "this")
  public static boolean shortTemplateEntry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shortTemplateEntry")) return false;
    if (!nextTokenIs(builder_, SHORT_TEMPLATE_ENTRY_START)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, SHORT_TEMPLATE_ENTRY_START);
    result_ = result_ && shortTemplateEntry_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, SHORT_TEMPLATE_ENTRY, result_);
    return result_;
  }

  // referenceExpression | "this"
  private static boolean shortTemplateEntry_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shortTemplateEntry_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, "this");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean simpleNameOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleNameOperation")) return false;
    if (!nextTokenIs(builder_, IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IDENTIFIER);
    exit_section_(builder_, marker_, SIMPLE_NAME_OPERATION, result_);
    return result_;
  }

  /* ********************************************************** */
  // referenceExpression typeArgumentList?
  public static boolean simpleUserType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleUserType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<simple user type>");
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && simpleUserType_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SIMPLE_USER_TYPE, result_, false, null);
    return result_;
  }

  // typeArgumentList?
  private static boolean simpleUserType_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleUserType_1")) return false;
    typeArgumentList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "." privateSimpleUserType
  public static boolean simpleUserTypeAdd(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleUserTypeAdd")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<simple user type add>");
    result_ = consumeToken(builder_, ".");
    result_ = result_ && privateSimpleUserType(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SIMPLE_USER_TYPE_ADD, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // simpleUserType simpleUserTypeAdd*
  static boolean simpleUserTypeExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleUserTypeExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = simpleUserType(builder_, level_ + 1);
    result_ = result_ && simpleUserTypeExt_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // simpleUserTypeAdd*
  private static boolean simpleUserTypeExt_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleUserTypeExt_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!simpleUserTypeAdd(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "simpleUserTypeExt_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // parameter
  public static boolean singleValueParameterList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "singleValueParameterList")) return false;
    if (!nextTokenIs(builder_, IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parameter(builder_, level_ + 1);
    exit_section_(builder_, marker_, SINGLE_VALUE_PARAMETER_LIST, result_);
    return result_;
  }

  /* ********************************************************** */
  // "(" annotationsPlus? parameter ")"
  public static boolean singleValueParameterListWithBrackets(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "singleValueParameterListWithBrackets")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<single value parameter list with brackets>");
    result_ = consumeToken(builder_, "(");
    result_ = result_ && singleValueParameterListWithBrackets_1(builder_, level_ + 1);
    result_ = result_ && parameter(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, level_, marker_, SINGLE_VALUE_PARAMETER_LIST_WITH_BRACKETS, result_, false, null);
    return result_;
  }

  // annotationsPlus?
  private static boolean singleValueParameterListWithBrackets_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "singleValueParameterListWithBrackets_1")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // declaration
  //   | expression
  static boolean statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = declaration(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (statement MULTISEMI)* statement?
  static boolean statementSEMIExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementSEMIExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = statementSEMIExt_0(builder_, level_ + 1);
    result_ = result_ && statementSEMIExt_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (statement MULTISEMI)*
  private static boolean statementSEMIExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementSEMIExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!statementSEMIExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "statementSEMIExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // statement MULTISEMI
  private static boolean statementSEMIExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementSEMIExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = statement(builder_, level_ + 1);
    result_ = result_ && MULTISEMI(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // statement?
  private static boolean statementSEMIExt_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementSEMIExt_1")) return false;
    statement(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // MULTISEMI? statementSEMIExt? MULTISEMI?
  static boolean statements(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statements")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = statements_0(builder_, level_ + 1);
    result_ = result_ && statements_1(builder_, level_ + 1);
    result_ = result_ && statements_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // MULTISEMI?
  private static boolean statements_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statements_0")) return false;
    MULTISEMI(builder_, level_ + 1);
    return true;
  }

  // statementSEMIExt?
  private static boolean statements_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statements_1")) return false;
    statementSEMIExt(builder_, level_ + 1);
    return true;
  }

  // MULTISEMI?
  private static boolean statements_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statements_2")) return false;
    MULTISEMI(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // statements
  public static boolean statementsBlock(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementsBlock")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<statements block>");
    result_ = statements(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, STATEMENTS_BLOCK, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "\"\"\"" stringTemplateElement* "\"\"\""
  //   | "\"" stringTemplateElement* "\""
  public static boolean stringTemplate(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringTemplate")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<string template>");
    result_ = stringTemplate_0(builder_, level_ + 1);
    if (!result_) result_ = stringTemplate_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, STRING_TEMPLATE, result_, false, null);
    return result_;
  }

  // "\"\"\"" stringTemplateElement* "\"\"\""
  private static boolean stringTemplate_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringTemplate_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "\"\"\"");
    result_ = result_ && stringTemplate_0_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "\"\"\"");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // stringTemplateElement*
  private static boolean stringTemplate_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringTemplate_0_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!stringTemplateElement(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "stringTemplate_0_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // "\"" stringTemplateElement* "\""
  private static boolean stringTemplate_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringTemplate_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "\"");
    result_ = result_ && stringTemplate_1_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "\"");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // stringTemplateElement*
  private static boolean stringTemplate_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringTemplate_1_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!stringTemplateElement(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "stringTemplate_1_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // literalStringTemplateEntry
  //   | shortTemplateEntry
  //   | ESCAPE_SEQUENCE
  //   | longTemplate
  static boolean stringTemplateElement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringTemplateElement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = literalStringTemplateEntry(builder_, level_ + 1);
    if (!result_) result_ = shortTemplateEntry(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, ESCAPE_SEQUENCE);
    if (!result_) result_ = longTemplate(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // superReference ("<" DISABLE_NEWLINES type RESTORE_NEWLINES_STATE ">")? label?
  public static boolean superExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "superExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<super expression>");
    result_ = superReference(builder_, level_ + 1);
    result_ = result_ && superExpression_1(builder_, level_ + 1);
    result_ = result_ && superExpression_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SUPER_EXPRESSION, result_, false, null);
    return result_;
  }

  // ("<" DISABLE_NEWLINES type RESTORE_NEWLINES_STATE ">")?
  private static boolean superExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "superExpression_1")) return false;
    superExpression_1_0(builder_, level_ + 1);
    return true;
  }

  // "<" DISABLE_NEWLINES type RESTORE_NEWLINES_STATE ">"
  private static boolean superExpression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "superExpression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "<");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && type(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ">");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // label?
  private static boolean superExpression_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "superExpression_2")) return false;
    label(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "super"
  public static boolean superReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "superReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<super reference>");
    result_ = consumeToken(builder_, "super");
    exit_section_(builder_, level_, marker_, SUPER_REFERENCE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (block | expression)?
  public static boolean thenExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "thenExpression")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<then expression>");
    thenExpression_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, THEN_EXPRESSION, true, false, null);
    return true;
  }

  // block | expression
  private static boolean thenExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "thenExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = block(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (block | expression)? SEMI?
  public static boolean thenExpressionWithSemi(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "thenExpressionWithSemi")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<then expression with semi>");
    result_ = thenExpressionWithSemi_0(builder_, level_ + 1);
    result_ = result_ && thenExpressionWithSemi_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, THEN_EXPRESSION_WITH_SEMI, result_, false, null);
    return result_;
  }

  // (block | expression)?
  private static boolean thenExpressionWithSemi_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "thenExpressionWithSemi_0")) return false;
    thenExpressionWithSemi_0_0(builder_, level_ + 1);
    return true;
  }

  // block | expression
  private static boolean thenExpressionWithSemi_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "thenExpressionWithSemi_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = block(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // SEMI?
  private static boolean thenExpressionWithSemi_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "thenExpressionWithSemi_1")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // thisReference
  public static boolean thisExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "thisExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<this expression>");
    result_ = thisReference(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, THIS_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "this"
  public static boolean thisReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "thisReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<this reference>");
    result_ = consumeToken(builder_, "this");
    exit_section_(builder_, level_, marker_, THIS_REFERENCE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // packageDirective
  //   | classDeclaration
  //   | object
  //   | function
  //   | property
  //   | typedef
  static boolean toplevelObject(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "toplevelObject")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = packageDirective(builder_, level_ + 1);
    if (!result_) result_ = classDeclaration(builder_, level_ + 1);
    if (!result_) result_ = object(builder_, level_ + 1);
    if (!result_) result_ = function(builder_, level_ + 1);
    if (!result_) result_ = property(builder_, level_ + 1);
    if (!result_) result_ = typedef(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "try" block catchBlock* finallyBlock?
  public static boolean tryBlock(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryBlock")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<try block>");
    result_ = consumeToken(builder_, "try");
    result_ = result_ && block(builder_, level_ + 1);
    result_ = result_ && tryBlock_2(builder_, level_ + 1);
    result_ = result_ && tryBlock_3(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TRY_BLOCK, result_, false, null);
    return result_;
  }

  // catchBlock*
  private static boolean tryBlock_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryBlock_2")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!catchBlock(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "tryBlock_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // finallyBlock?
  private static boolean tryBlock_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryBlock_3")) return false;
    finallyBlock(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // typeDescriptor
  public static boolean type(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type>");
    result_ = typeDescriptor(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "<" DISABLE_NEWLINES optionalProjectionTypeExt RESTORE_NEWLINES_STATE">"
  public static boolean typeArgumentList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeArgumentList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type argument list>");
    result_ = consumeToken(builder_, "<");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && optionalProjectionTypeExt(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ">");
    exit_section_(builder_, level_, marker_, TYPE_ARGUMENT_LIST, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "<" DISABLE_NEWLINES typeList ENABLE_NEWLINES ">"
  public static boolean typeArguments(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeArguments")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type arguments>");
    result_ = consumeToken(builder_, "<");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && typeList(builder_, level_ + 1);
    result_ = result_ && enableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && consumeToken(builder_, ">");
    exit_section_(builder_, level_, marker_, TYPE_ARGUMENTS, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // annotationsPlus? referenceExpression ":" type
  //   | annotationsPlus? "class" "object" referenceExpression ":" type
  public static boolean typeConstraint(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeConstraint")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type constraint>");
    result_ = typeConstraint_0(builder_, level_ + 1);
    if (!result_) result_ = typeConstraint_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE_CONSTRAINT, result_, false, null);
    return result_;
  }

  // annotationsPlus? referenceExpression ":" type
  private static boolean typeConstraint_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeConstraint_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeConstraint_0_0(builder_, level_ + 1);
    result_ = result_ && referenceExpression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean typeConstraint_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeConstraint_0_0")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  // annotationsPlus? "class" "object" referenceExpression ":" type
  private static boolean typeConstraint_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeConstraint_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeConstraint_1_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "class");
    result_ = result_ && consumeToken(builder_, "object");
    result_ = result_ && referenceExpression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean typeConstraint_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeConstraint_1_0")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // (typeConstraint ",")* typeConstraint
  public static boolean typeConstraintExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeConstraintExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type constraint ext>");
    result_ = typeConstraintExt_0(builder_, level_ + 1);
    result_ = result_ && typeConstraint(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE_CONSTRAINT_EXT, result_, false, null);
    return result_;
  }

  // (typeConstraint ",")*
  private static boolean typeConstraintExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeConstraintExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!typeConstraintExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "typeConstraintExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // typeConstraint ","
  private static boolean typeConstraintExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeConstraintExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeConstraint(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<at WHERE_KEYWORD>> "where" typeConstraintExt
  static boolean typeConstraints(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeConstraints")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, WHERE_KEYWORD);
    result_ = result_ && consumeToken(builder_, "where");
    result_ = result_ && typeConstraintExt(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // functionTypeReceiverReference ("." leftFunctionType)+
  //   | annotationsPlus? typeDescriptor2
  static boolean typeDescriptor(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptor")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeDescriptor_0(builder_, level_ + 1);
    if (!result_) result_ = typeDescriptor_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // functionTypeReceiverReference ("." leftFunctionType)+
  private static boolean typeDescriptor_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptor_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionTypeReceiverReference(builder_, level_ + 1);
    result_ = result_ && typeDescriptor_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ("." leftFunctionType)+
  private static boolean typeDescriptor_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptor_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeDescriptor_0_1_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!typeDescriptor_0_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "typeDescriptor_0_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "." leftFunctionType
  private static boolean typeDescriptor_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptor_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ".");
    result_ = result_ && leftFunctionType(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus? typeDescriptor2
  private static boolean typeDescriptor_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptor_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeDescriptor_1_0(builder_, level_ + 1);
    result_ = result_ && typeDescriptor2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // annotationsPlus?
  private static boolean typeDescriptor_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptor_1_0")) return false;
    annotationsPlus(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // typeDescriptorAfterRecursion nullableType*
  static boolean typeDescriptor2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptor2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeDescriptorAfterRecursion(builder_, level_ + 1);
    result_ = result_ && typeDescriptor2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // nullableType*
  private static boolean typeDescriptor2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptor2_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!nullableType(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "typeDescriptor2_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // functionType
  //   | typeDescriptorInBrackets
  //   | userType
  //   | selfType
  static boolean typeDescriptorAfterRecursion(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptorAfterRecursion")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionType(builder_, level_ + 1);
    if (!result_) result_ = typeDescriptorInBrackets(builder_, level_ + 1);
    if (!result_) result_ = userType(builder_, level_ + 1);
    if (!result_) result_ = selfType(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "(" DISABLE_NEWLINES typeDescriptor RESTORE_NEWLINES_STATE ")"
  static boolean typeDescriptorInBrackets(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeDescriptorInBrackets")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && typeDescriptor(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // modifierList? type ("," modifierList? type)*
  public static boolean typeList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type list>");
    result_ = typeList_0(builder_, level_ + 1);
    result_ = result_ && type(builder_, level_ + 1);
    result_ = result_ && typeList_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE_LIST, result_, false, null);
    return result_;
  }

  // modifierList?
  private static boolean typeList_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // ("," modifierList? type)*
  private static boolean typeList_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList_2")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!typeList_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "typeList_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // "," modifierList? type
  private static boolean typeList_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ",");
    result_ = result_ && typeList_2_0_1(builder_, level_ + 1);
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // modifierList?
  private static boolean typeList_2_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList_2_0_1")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "as" | "as?" | ":"
  public static boolean typeOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type operation>");
    result_ = consumeToken(builder_, "as");
    if (!result_) result_ = consumeToken(builder_, "as?");
    if (!result_) result_ = consumeToken(builder_, ":");
    exit_section_(builder_, level_, marker_, TYPE_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // <<stopInTypeParameter marker_>> ALLOW_SHORT_ANNOTATIONS modifierList? RESTORE_ANNOTATIONS_STATE <<unStop>> IDENTIFIER_NEXT IDENTIFIER (":" type)?
  public static boolean typeParameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParameter")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type parameter>");
    result_ = stopInTypeParameter(builder_, level_ + 1, marker_);
    result_ = result_ && allowShortAnnotations(builder_, level_ + 1, marker_);
    result_ = result_ && typeParameter_2(builder_, level_ + 1);
    result_ = result_ && restoreAnnotationsState(builder_, level_ + 1);
    result_ = result_ && unStop(builder_, level_ + 1);
    result_ = result_ && identifierNext(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    result_ = result_ && typeParameter_7(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE_PARAMETER, result_, false, null);
    return result_;
  }

  // modifierList?
  private static boolean typeParameter_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParameter_2")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // (":" type)?
  private static boolean typeParameter_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParameter_7")) return false;
    typeParameter_7_0(builder_, level_ + 1);
    return true;
  }

  // ":" type
  private static boolean typeParameter_7_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParameter_7_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (typeParameter ",")* typeParameter
  static boolean typeParameterExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParameterExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeParameterExt_0(builder_, level_ + 1);
    result_ = result_ && typeParameter(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (typeParameter ",")*
  private static boolean typeParameterExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParameterExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!typeParameterExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "typeParameterExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // typeParameter ","
  private static boolean typeParameterExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParameterExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeParameter(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "<" DISABLE_NEWLINES typeParameterExt RESTORE_NEWLINES_STATE">"
  public static boolean typeParameters(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParameters")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type parameters>");
    result_ = consumeToken(builder_, "<");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && typeParameterExt(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ">");
    exit_section_(builder_, level_, marker_, TYPE_PARAMETERS, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // modifierList? type
  public static boolean typeProjection(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeProjection")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type projection>");
    result_ = typeProjection_0(builder_, level_ + 1);
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE_PROJECTION, result_, false, null);
    return result_;
  }

  // modifierList?
  private static boolean typeProjection_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeProjection_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // prefixUnaryExpression typeRHSPlus*
  static boolean typeRHS(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeRHS")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = prefixUnaryExpression(builder_, level_ + 1);
    result_ = result_ && typeRHS_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeRHSPlus*
  private static boolean typeRHS_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeRHS_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!typeRHSPlus(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "typeRHS_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // typeOperation type
  public static boolean typeRHSPlus(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeRHSPlus")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<type rhs plus>");
    result_ = typeOperation(builder_, level_ + 1);
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE_RHS_PLUS, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // modifierListWithShortAnnotations? "type" IDENTIFIER (typeParameters typeConstraints?)? "=" type SEMI?
  public static boolean typedef(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedef")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<typedef>");
    result_ = typedef_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "type");
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    result_ = result_ && typedef_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "=");
    result_ = result_ && type(builder_, level_ + 1);
    result_ = result_ && typedef_6(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPEDEF, result_, false, null);
    return result_;
  }

  // modifierListWithShortAnnotations?
  private static boolean typedef_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedef_0")) return false;
    modifierListWithShortAnnotations(builder_, level_ + 1);
    return true;
  }

  // (typeParameters typeConstraints?)?
  private static boolean typedef_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedef_3")) return false;
    typedef_3_0(builder_, level_ + 1);
    return true;
  }

  // typeParameters typeConstraints?
  private static boolean typedef_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedef_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeParameters(builder_, level_ + 1);
    result_ = result_ && typedef_3_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // typeConstraints?
  private static boolean typedef_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedef_3_0_1")) return false;
    typeConstraints(builder_, level_ + 1);
    return true;
  }

  // SEMI?
  private static boolean typedef_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedef_6")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // ("package" ".")? simpleUserTypeExt
  static boolean userType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = userType_0(builder_, level_ + 1);
    result_ = result_ && simpleUserTypeExt(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ("package" ".")?
  private static boolean userType_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userType_0")) return false;
    userType_0_0(builder_, level_ + 1);
    return true;
  }

  // "package" "."
  private static boolean userType_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userType_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "package");
    result_ = result_ && consumeToken(builder_, ".");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // userType
  public static boolean userTypeReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userTypeReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<user type reference>");
    result_ = userType(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, USER_TYPE_REFERENCE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (valueArgumentName "=")? "*"? expression
  public static boolean valueArgument(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArgument")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<value argument>");
    result_ = valueArgument_0(builder_, level_ + 1);
    result_ = result_ && valueArgument_1(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, VALUE_ARGUMENT, result_, false, null);
    return result_;
  }

  // (valueArgumentName "=")?
  private static boolean valueArgument_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArgument_0")) return false;
    valueArgument_0_0(builder_, level_ + 1);
    return true;
  }

  // valueArgumentName "="
  private static boolean valueArgument_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArgument_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = valueArgumentName(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "=");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // "*"?
  private static boolean valueArgument_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArgument_1")) return false;
    consumeToken(builder_, "*");
    return true;
  }

  /* ********************************************************** */
  // (valueArgument ",")* valueArgument
  static boolean valueArgumentList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArgumentList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = valueArgumentList_0(builder_, level_ + 1);
    result_ = result_ && valueArgument(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (valueArgument ",")*
  private static boolean valueArgumentList_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArgumentList_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!valueArgumentList_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "valueArgumentList_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // valueArgument ","
  private static boolean valueArgumentList_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArgumentList_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = valueArgument(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // referenceExpression
  public static boolean valueArgumentName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArgumentName")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<value argument name>");
    result_ = referenceExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, VALUE_ARGUMENT_NAME, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "(" DISABLE_NEWLINES valueArgumentList? RESTORE_NEWLINES_STATE ")"
  public static boolean valueArguments(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArguments")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<value arguments>");
    result_ = consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && valueArguments_2(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, level_, marker_, VALUE_ARGUMENTS, result_, false, null);
    return result_;
  }

  // valueArgumentList?
  private static boolean valueArguments_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueArguments_2")) return false;
    valueArgumentList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "(" DISABLE_NEWLINES parameterModifiersTypeExt? RESTORE_NEWLINES_STATE")"
  public static boolean valueParameterList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameterList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<value parameter list>");
    result_ = consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && valueParameterList_2(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, level_, marker_, VALUE_PARAMETER_LIST, result_, false, null);
    return result_;
  }

  // parameterModifiersTypeExt?
  private static boolean valueParameterList_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameterList_2")) return false;
    parameterModifiersTypeExt(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // modifierList? IDENTIFIER
  public static boolean valueParameterNoType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameterNoType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<value parameter no type>");
    result_ = valueParameterNoType_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    exit_section_(builder_, level_, marker_, VALUE_PARAMETER_NO_TYPE, result_, false, null);
    return result_;
  }

  // modifierList?
  private static boolean valueParameterNoType_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameterNoType_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // modifierList? IDENTIFIER (":" type)?
  public static boolean valueParameterWithType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameterWithType")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<value parameter with type>");
    result_ = valueParameterWithType_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    result_ = result_ && valueParameterWithType_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, VALUE_PARAMETER_WITH_TYPE, result_, false, null);
    return result_;
  }

  // modifierList?
  private static boolean valueParameterWithType_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameterWithType_0")) return false;
    modifierList(builder_, level_ + 1);
    return true;
  }

  // (":" type)?
  private static boolean valueParameterWithType_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameterWithType_2")) return false;
    valueParameterWithType_2_0(builder_, level_ + 1);
    return true;
  }

  // ":" type
  private static boolean valueParameterWithType_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameterWithType_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "(" DISABLE_NEWLINES functionParameterExt? RESTORE_NEWLINES_STATE ")"
  public static boolean valueParameters(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameters")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<value parameters>");
    result_ = consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && valueParameters_2(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, level_, marker_, VALUE_PARAMETERS, result_, false, null);
    return result_;
  }

  // functionParameterExt?
  private static boolean valueParameters_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueParameters_2")) return false;
    functionParameterExt(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER (":" type)?
  static boolean variableDeclarationEntry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variableDeclarationEntry")) return false;
    if (!nextTokenIs(builder_, IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IDENTIFIER);
    result_ = result_ && variableDeclarationEntry_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (":" type)?
  private static boolean variableDeclarationEntry_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variableDeclarationEntry_1")) return false;
    variableDeclarationEntry_1_0(builder_, level_ + 1);
    return true;
  }

  // ":" type
  private static boolean variableDeclarationEntry_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variableDeclarationEntry_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (variableDeclarationEntry ",")* variableDeclarationEntry
  public static boolean variableDeclarationEntryExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variableDeclarationEntryExt")) return false;
    if (!nextTokenIs(builder_, IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = variableDeclarationEntryExt_0(builder_, level_ + 1);
    result_ = result_ && variableDeclarationEntry(builder_, level_ + 1);
    exit_section_(builder_, marker_, VARIABLE_DECLARATION_ENTRY_EXT, result_);
    return result_;
  }

  // (variableDeclarationEntry ",")*
  private static boolean variableDeclarationEntryExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variableDeclarationEntryExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!variableDeclarationEntryExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "variableDeclarationEntryExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // variableDeclarationEntry ","
  private static boolean variableDeclarationEntryExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variableDeclarationEntryExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = variableDeclarationEntry(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<at IN_KEYWORD>> "in"
  //   | <<at OUT_KEYWORD>> "out"
  static boolean varianceAnnotation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varianceAnnotation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = varianceAnnotation_0(builder_, level_ + 1);
    if (!result_) result_ = varianceAnnotation_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at IN_KEYWORD>> "in"
  private static boolean varianceAnnotation_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varianceAnnotation_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, IN_KEYWORD);
    result_ = result_ && consumeToken(builder_, "in");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<at OUT_KEYWORD>> "out"
  private static boolean varianceAnnotation_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varianceAnnotation_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = at(builder_, level_ + 1, OUT_KEYWORD);
    result_ = result_ && consumeToken(builder_, "out");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "when"
  //   //("(" DISABLE_NEWLINES (modifierListWithShortAnnotations? "val" IDENTIFIER "=")? expression RESTORE_NEWLINES_STATE ")")?
  //   ("(" DISABLE_NEWLINES (whenProperty | expression) RESTORE_NEWLINES_STATE ")")?
  //       //("(" expression | propertyLocal ")")?
  //     "{"
  //         ENABLE_NEWLINES whenEntry* RESTORE_NEWLINES_STATE
  //     "}"
  public static boolean when(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "when")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<when>");
    result_ = consumeToken(builder_, "when");
    result_ = result_ && when_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "{");
    result_ = result_ && enableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && when_4(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "}");
    exit_section_(builder_, level_, marker_, WHEN, result_, false, null);
    return result_;
  }

  // ("(" DISABLE_NEWLINES (whenProperty | expression) RESTORE_NEWLINES_STATE ")")?
  private static boolean when_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "when_1")) return false;
    when_1_0(builder_, level_ + 1);
    return true;
  }

  // "(" DISABLE_NEWLINES (whenProperty | expression) RESTORE_NEWLINES_STATE ")"
  private static boolean when_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "when_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "(");
    result_ = result_ && disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && when_1_0_2(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // whenProperty | expression
  private static boolean when_1_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "when_1_0_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = whenProperty(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // whenEntry*
  private static boolean when_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "when_4")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!whenEntry(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "when_4", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // DISABLE_NEWLINES (whenConditionExpression
  //   | whenConditionInRange
  //   | whenConditionIsPattern) RESTORE_NEWLINES_STATE
  static boolean whenCondition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenCondition")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = disableNewlines(builder_, level_ + 1, marker_);
    result_ = result_ && whenCondition_1(builder_, level_ + 1);
    result_ = result_ && restoreNewlinesState(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // whenConditionExpression
  //   | whenConditionInRange
  //   | whenConditionIsPattern
  private static boolean whenCondition_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenCondition_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = whenConditionExpression(builder_, level_ + 1);
    if (!result_) result_ = whenConditionInRange(builder_, level_ + 1);
    if (!result_) result_ = whenConditionIsPattern(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // expression
  public static boolean whenConditionExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenConditionExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<when condition expression>");
    result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, WHEN_CONDITION_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (whenCondition ",")* whenCondition
  static boolean whenConditionExt(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenConditionExt")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = whenConditionExt_0(builder_, level_ + 1);
    result_ = result_ && whenCondition(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (whenCondition ",")*
  private static boolean whenConditionExt_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenConditionExt_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!whenConditionExt_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "whenConditionExt_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // whenCondition ","
  private static boolean whenConditionExt_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenConditionExt_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = whenCondition(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ",");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // inOperation expression
  public static boolean whenConditionInRange(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenConditionInRange")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<when condition in range>");
    result_ = inOperation(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, WHEN_CONDITION_IN_RANGE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // ("is" | "!is") isRHS
  public static boolean whenConditionIsPattern(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenConditionIsPattern")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<when condition is pattern>");
    result_ = whenConditionIsPattern_0(builder_, level_ + 1);
    result_ = result_ && isRHS(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, WHEN_CONDITION_IS_PATTERN, result_, false, null);
    return result_;
  }

  // "is" | "!is"
  private static boolean whenConditionIsPattern_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenConditionIsPattern_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "is");
    if (!result_) result_ = consumeToken(builder_, "!is");
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // whenConditionExt "->" expression SEMI?
  //   | "else" "->" expression SEMI?
  public static boolean whenEntry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenEntry")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<when entry>");
    result_ = whenEntry_0(builder_, level_ + 1);
    if (!result_) result_ = whenEntry_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, WHEN_ENTRY, result_, false, null);
    return result_;
  }

  // whenConditionExt "->" expression SEMI?
  private static boolean whenEntry_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenEntry_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = whenConditionExt(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "->");
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && whenEntry_0_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // SEMI?
  private static boolean whenEntry_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenEntry_0_3")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  // "else" "->" expression SEMI?
  private static boolean whenEntry_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenEntry_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, "else");
    result_ = result_ && consumeToken(builder_, "->");
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && whenEntry_1_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // SEMI?
  private static boolean whenEntry_1_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenEntry_1_3")) return false;
    SEMI(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // modifierListWithShortAnnotations? "val" IDENTIFIER "=" expression
  public static boolean whenProperty(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenProperty")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<when property>");
    result_ = whenProperty_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, "val");
    result_ = result_ && consumeToken(builder_, IDENTIFIER);
    result_ = result_ && consumeToken(builder_, "=");
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, WHEN_PROPERTY, result_, false, null);
    return result_;
  }

  // modifierListWithShortAnnotations?
  private static boolean whenProperty_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whenProperty_0")) return false;
    modifierListWithShortAnnotations(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "while" "(" condition ")" body
  public static boolean whileLoop(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whileLoop")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<while loop>");
    result_ = consumeToken(builder_, "while");
    result_ = result_ && consumeToken(builder_, "(");
    result_ = result_ && condition(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ")");
    result_ = result_ && body(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, WHILE_LOOP, result_, false, null);
    return result_;
  }

}
