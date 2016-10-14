/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IErrorCounterReparseableElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.lexer.KotlinLexer;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.parsing.KotlinParser;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public interface KtNodeTypes {
    IFileElementType KT_FILE = new IFileElementType(KotlinLanguage.INSTANCE);

    IElementType CLASS     = KtStubElementTypes.CLASS;
    IElementType FUN       = KtStubElementTypes.FUNCTION;
    IElementType PROPERTY  = KtStubElementTypes.PROPERTY;
    IElementType DESTRUCTURING_DECLARATION = new KtNodeType("DESTRUCTURING_DECLARATION", KtDestructuringDeclaration.class);
    IElementType DESTRUCTURING_DECLARATION_ENTRY = new KtNodeType("DESTRUCTURING_DECLARATION_ENTRY", KtDestructuringDeclarationEntry.class);

    IElementType OBJECT_DECLARATION = KtStubElementTypes.OBJECT_DECLARATION;
    IElementType TYPEALIAS = KtStubElementTypes.TYPEALIAS;

    IElementType ENUM_ENTRY            = KtStubElementTypes.ENUM_ENTRY;
    IElementType CLASS_INITIALIZER = KtStubElementTypes.CLASS_INITIALIZER;
    IElementType SCRIPT_INITIALIZER    = new KtNodeType("SCRIPT_INITIALIZER", KtScriptInitializer.class);
    IElementType SECONDARY_CONSTRUCTOR  = KtStubElementTypes.SECONDARY_CONSTRUCTOR;
    IElementType PRIMARY_CONSTRUCTOR    = KtStubElementTypes.PRIMARY_CONSTRUCTOR;

    IElementType TYPE_PARAMETER_LIST                 = KtStubElementTypes.TYPE_PARAMETER_LIST;
    IElementType TYPE_PARAMETER                      = KtStubElementTypes.TYPE_PARAMETER;
    IElementType SUPER_TYPE_LIST                     = KtStubElementTypes.SUPER_TYPE_LIST;
    IElementType DELEGATED_SUPER_TYPE_ENTRY          = KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY;
    IElementType SUPER_TYPE_CALL_ENTRY               = KtStubElementTypes.SUPER_TYPE_CALL_ENTRY;
    IElementType SUPER_TYPE_ENTRY                    = KtStubElementTypes.SUPER_TYPE_ENTRY;
    KtNodeType PROPERTY_DELEGATE                    = new KtNodeType("PROPERTY_DELEGATE", KtPropertyDelegate.class);
    IElementType CONSTRUCTOR_CALLEE                  = KtStubElementTypes.CONSTRUCTOR_CALLEE;
    IElementType VALUE_PARAMETER_LIST               = KtStubElementTypes.VALUE_PARAMETER_LIST;
    IElementType VALUE_PARAMETER                    = KtStubElementTypes.VALUE_PARAMETER;

    IElementType CLASS_BODY                         = KtStubElementTypes.CLASS_BODY;
    IElementType IMPORT_LIST                        = KtStubElementTypes.IMPORT_LIST;
    IElementType FILE_ANNOTATION_LIST               = KtStubElementTypes.FILE_ANNOTATION_LIST;
    IElementType IMPORT_DIRECTIVE                   = KtStubElementTypes.IMPORT_DIRECTIVE;
    IElementType MODIFIER_LIST                      = KtStubElementTypes.MODIFIER_LIST;
    IElementType ANNOTATION                         = KtStubElementTypes.ANNOTATION;
    IElementType ANNOTATION_ENTRY                   = KtStubElementTypes.ANNOTATION_ENTRY;
    IElementType ANNOTATION_TARGET                  = KtStubElementTypes.ANNOTATION_TARGET;

    IElementType TYPE_ARGUMENT_LIST                 = KtStubElementTypes.TYPE_ARGUMENT_LIST;
    KtNodeType VALUE_ARGUMENT_LIST                 = new KtNodeType("VALUE_ARGUMENT_LIST", KtValueArgumentList.class);
    KtNodeType VALUE_ARGUMENT                      = new KtNodeType("VALUE_ARGUMENT", KtValueArgument.class);
    KtNodeType LAMBDA_ARGUMENT                     = new KtNodeType("LAMBDA_ARGUMENT", KtLambdaArgument.class);
    KtNodeType VALUE_ARGUMENT_NAME                 = new KtNodeType("VALUE_ARGUMENT_NAME", KtValueArgumentName.class);
    IElementType TYPE_REFERENCE                     = KtStubElementTypes.TYPE_REFERENCE;

    IElementType USER_TYPE                = KtStubElementTypes.USER_TYPE;
    IElementType DYNAMIC_TYPE             = KtStubElementTypes.DYNAMIC_TYPE;
    IElementType FUNCTION_TYPE            = KtStubElementTypes.FUNCTION_TYPE;
    IElementType FUNCTION_TYPE_RECEIVER   = KtStubElementTypes.FUNCTION_TYPE_RECEIVER;
    KtNodeType SELF_TYPE     = new KtNodeType("SELF_TYPE", KtSelfType.class);
    IElementType NULLABLE_TYPE            = KtStubElementTypes.NULLABLE_TYPE;
    IElementType TYPE_PROJECTION          = KtStubElementTypes.TYPE_PROJECTION;

    // TODO: review
    IElementType PROPERTY_ACCESSOR       = KtStubElementTypes.PROPERTY_ACCESSOR;
    IElementType INITIALIZER_LIST        = KtStubElementTypes.INITIALIZER_LIST;
    IElementType TYPE_CONSTRAINT_LIST    = KtStubElementTypes.TYPE_CONSTRAINT_LIST;
    IElementType TYPE_CONSTRAINT         = KtStubElementTypes.TYPE_CONSTRAINT;

    IElementType CONSTRUCTOR_DELEGATION_CALL = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_CALL", KtConstructorDelegationCall.class);
    KtNodeType CONSTRUCTOR_DELEGATION_REFERENCE = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_REFERENCE", KtConstructorDelegationReferenceExpression.class);

    // TODO: Not sure if we need separate NT for each kind of constants
    KtNodeType NULL               = new KtNodeType("NULL", KtConstantExpression.class);
    KtNodeType BOOLEAN_CONSTANT   = new KtNodeType("BOOLEAN_CONSTANT", KtConstantExpression.class);
    KtNodeType FLOAT_CONSTANT     = new KtNodeType("FLOAT_CONSTANT", KtConstantExpression.class);
    KtNodeType CHARACTER_CONSTANT = new KtNodeType("CHARACTER_CONSTANT", KtConstantExpression.class);
    KtNodeType INTEGER_CONSTANT   = new KtNodeType("INTEGER_CONSTANT", KtConstantExpression.class);

    KtNodeType STRING_TEMPLATE    = new KtNodeType("STRING_TEMPLATE", KtStringTemplateExpression.class);
    KtNodeType LONG_STRING_TEMPLATE_ENTRY = new KtNodeType("LONG_STRING_TEMPLATE_ENTRY", KtBlockStringTemplateEntry.class);
    KtNodeType SHORT_STRING_TEMPLATE_ENTRY = new KtNodeType("SHORT_STRING_TEMPLATE_ENTRY", KtSimpleNameStringTemplateEntry.class);
    KtNodeType LITERAL_STRING_TEMPLATE_ENTRY = new KtNodeType("LITERAL_STRING_TEMPLATE_ENTRY", KtLiteralStringTemplateEntry.class);
    KtNodeType ESCAPE_STRING_TEMPLATE_ENTRY = new KtNodeType("ESCAPE_STRING_TEMPLATE_ENTRY", KtEscapeStringTemplateEntry.class);

    KtNodeType PARENTHESIZED             = new KtNodeType("PARENTHESIZED", KtParenthesizedExpression.class);
    KtNodeType RETURN                    = new KtNodeType("RETURN", KtReturnExpression.class);
    KtNodeType THROW                     = new KtNodeType("THROW", KtThrowExpression.class);
    KtNodeType CONTINUE                  = new KtNodeType("CONTINUE", KtContinueExpression.class);
    KtNodeType BREAK                     = new KtNodeType("BREAK", KtBreakExpression.class);
    KtNodeType IF                        = new KtNodeType("IF", KtIfExpression.class);
    KtNodeType CONDITION                 = new KtNodeType("CONDITION", KtContainerNode.class);
    KtNodeType THEN                      = new KtNodeType("THEN", KtContainerNodeForControlStructureBody.class);
    KtNodeType ELSE                      = new KtNodeType("ELSE", KtContainerNodeForControlStructureBody.class);
    KtNodeType TRY                       = new KtNodeType("TRY", KtTryExpression.class);
    KtNodeType CATCH                     = new KtNodeType("CATCH", KtCatchClause.class);
    KtNodeType FINALLY                   = new KtNodeType("FINALLY", KtFinallySection.class);
    KtNodeType FOR                       = new KtNodeType("FOR", KtForExpression.class);
    KtNodeType WHILE                     = new KtNodeType("WHILE", KtWhileExpression.class);
    KtNodeType DO_WHILE                  = new KtNodeType("DO_WHILE", KtDoWhileExpression.class);
    KtNodeType LOOP_RANGE                = new KtNodeType("LOOP_RANGE", KtContainerNode.class);
    KtNodeType BODY                      = new KtNodeType("BODY", KtContainerNodeForControlStructureBody.class);
    KtNodeType BLOCK                     = new KtNodeType("BLOCK", KtBlockExpression.class);

    IElementType LAMBDA_EXPRESSION = new IErrorCounterReparseableElementType("LAMBDA_EXPRESSION", KotlinLanguage.INSTANCE) {
        @Override
        public ASTNode parseContents(ASTNode chameleon) {
            Project project = chameleon.getPsi().getProject();
            PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, KotlinLanguage.INSTANCE, chameleon.getChars());
            return KotlinParser.parseLambdaExpression(builder).getFirstChildNode();
        }

        @Nullable
        @Override
        public ASTNode createNode(CharSequence text) {
            return new KtLambdaExpression(text);
        }

        @Override
        public int getErrorsCount(CharSequence seq, Language fileLanguage, Project project) {
            Lexer lexer = new KotlinLexer();

            lexer.start(seq);
            if (lexer.getTokenType() != KtTokens.LBRACE) return IErrorCounterReparseableElementType.FATAL_ERROR;
            lexer.advance();
            int balance = 1;
            while (true) {
                IElementType type = lexer.getTokenType();
                if (type == null) break;
                if (balance == 0) {
                    return IErrorCounterReparseableElementType.FATAL_ERROR;
                }
                if (type == KtTokens.LBRACE) {
                    balance++;
                }
                else if (type == KtTokens.RBRACE) {
                    balance--;
                }
                lexer.advance();
            }
            return balance;
        }
    };


    KtNodeType FUNCTION_LITERAL          = new KtNodeType("FUNCTION_LITERAL", KtFunctionLiteral.class);
    KtNodeType ANNOTATED_EXPRESSION      = new KtNodeType("ANNOTATED_EXPRESSION", KtAnnotatedExpression.class);

    IElementType REFERENCE_EXPRESSION     = KtStubElementTypes.REFERENCE_EXPRESSION;
    IElementType ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION = KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION;
    KtNodeType OPERATION_REFERENCE       = new KtNodeType("OPERATION_REFERENCE", KtOperationReferenceExpression.class);
    KtNodeType LABEL                     = new KtNodeType("LABEL", KtLabelReferenceExpression.class);

    KtNodeType LABEL_QUALIFIER           = new KtNodeType("LABEL_QUALIFIER", KtContainerNode.class);

    KtNodeType THIS_EXPRESSION           = new KtNodeType("THIS_EXPRESSION", KtThisExpression.class);
    KtNodeType SUPER_EXPRESSION          = new KtNodeType("SUPER_EXPRESSION", KtSuperExpression.class);
    KtNodeType BINARY_EXPRESSION         = new KtNodeType("BINARY_EXPRESSION", KtBinaryExpression.class);
    KtNodeType BINARY_WITH_TYPE          = new KtNodeType("BINARY_WITH_TYPE", KtBinaryExpressionWithTypeRHS.class);
    KtNodeType IS_EXPRESSION = new KtNodeType("IS_EXPRESSION", KtIsExpression.class); // TODO:
    KtNodeType PREFIX_EXPRESSION         = new KtNodeType("PREFIX_EXPRESSION", KtPrefixExpression.class);
    KtNodeType POSTFIX_EXPRESSION        = new KtNodeType("POSTFIX_EXPRESSION", KtPostfixExpression.class);
    KtNodeType LABELED_EXPRESSION         = new KtNodeType("LABELED_EXPRESSION", KtLabeledExpression.class);
    KtNodeType CALL_EXPRESSION           = new KtNodeType("CALL_EXPRESSION", KtCallExpression.class);
    KtNodeType ARRAY_ACCESS_EXPRESSION   = new KtNodeType("ARRAY_ACCESS_EXPRESSION", KtArrayAccessExpression.class);
    KtNodeType INDICES                   = new KtNodeType("INDICES", KtContainerNode.class);
    IElementType DOT_QUALIFIED_EXPRESSION = KtStubElementTypes.DOT_QUALIFIED_EXPRESSION;
    KtNodeType CALLABLE_REFERENCE_EXPRESSION = new KtNodeType("CALLABLE_REFERENCE_EXPRESSION", KtCallableReferenceExpression.class);
    KtNodeType CLASS_LITERAL_EXPRESSION  = new KtNodeType("CLASS_LITERAL_EXPRESSION", KtClassLiteralExpression.class);
    KtNodeType SAFE_ACCESS_EXPRESSION    = new KtNodeType("SAFE_ACCESS_EXPRESSION", KtSafeQualifiedExpression.class);

    KtNodeType OBJECT_LITERAL            = new KtNodeType("OBJECT_LITERAL", KtObjectLiteralExpression.class);

    KtNodeType WHEN                      = new KtNodeType("WHEN", KtWhenExpression.class);
    KtNodeType WHEN_ENTRY                = new KtNodeType("WHEN_ENTRY", KtWhenEntry.class);

    KtNodeType WHEN_CONDITION_IN_RANGE   = new KtNodeType("WHEN_CONDITION_IN_RANGE", KtWhenConditionInRange.class);
    KtNodeType WHEN_CONDITION_IS_PATTERN = new KtNodeType("WHEN_CONDITION_IS_PATTERN", KtWhenConditionIsPattern.class);
    KtNodeType WHEN_CONDITION_EXPRESSION = new KtNodeType("WHEN_CONDITION_WITH_EXPRESSION", KtWhenConditionWithExpression.class);

    IElementType PACKAGE_DIRECTIVE = KtStubElementTypes.PACKAGE_DIRECTIVE;

    IElementType SCRIPT = KtStubElementTypes.SCRIPT;

    IFileElementType TYPE_CODE_FRAGMENT = new KtTypeCodeFragmentType();
    IFileElementType EXPRESSION_CODE_FRAGMENT = new KtExpressionCodeFragmentType();
    IFileElementType BLOCK_CODE_FRAGMENT = new KtBlockCodeFragmentType();
}
