package kt;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import generated.KotlinParser;
import generated.KotlinTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeType;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetLanguage;

public class KotlinParserDefinition implements ParserDefinition {

    //public static final Language KT_LANGUAGE = new Language("KT") {};
    //public static final Language KT_LANGUAGE = JetLanguage.INSTANCE;

    public static IElementType createType(String str) {
        if (str.equals("ADDITIVE_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("ASSIGNMENT_EXPRESSION")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("ADDITIVE_EXPRESSION_PLUS")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("ASSIGNMENT_OPERATOR")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("VALUE_PARAMETERS")) {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str.equals("FUNCTION")) {
            return JetNodeTypes.FUN;
        }
        if (str.equals("FUNCTION_LITERAL")) {
            return JetNodeTypes.FUNCTION_LITERAL;
        }
        if (str.equals("PACKAGE_HEADER")) {
            return JetNodeTypes.PACKAGE_DIRECTIVE;
        }
        if (str.equals("BLOCK")) {
            return JetNodeTypes.BLOCK;
        }
        if (str.equals("REFERENCE_EXPRESSION")) {
            return JetNodeTypes.REFERENCE_EXPRESSION;
        }
        if (str.equals("INTEGER_CONSTANT")) {
            return JetNodeTypes.INTEGER_CONSTANT;
        }
        if (str.equals("PROPERTY")) {
            return JetNodeTypes.PROPERTY;
        }
        if (str.equals("PROPERTY_LOCAL")) {
            return JetNodeTypes.PROPERTY;
        }
        if (str.equals("CLASS_DECLARATION")) {
            return JetNodeTypes.CLASS;
        }
        if (str.equals("DO_WHILE_LOOP")) {
            return JetNodeTypes.DO_WHILE;
        }
        if (str.equals("FOR_LOOP")) {
            return JetNodeTypes.FOR;
        }
        if (str.equals("ARRAY_ACCESS")) {
            return JetNodeTypes.ARRAY_ACCESS_EXPRESSION;
        }
        if (str.equals("IF_EXPRESSION")) {
            return JetNodeTypes.IF;
        }
        if (str.equals("CATCH_BLOCK")) {
            return JetNodeTypes.CATCH;
        }
        if (str.equals("WHILE_LOOP")) {
            return JetNodeTypes.WHILE;
        }
        if (str.equals("WHEN")) {
            return JetNodeTypes.WHEN;
        }
        if (str.equals("WHEN_ENTRY")) {
            return JetNodeTypes.WHEN_ENTRY;
        }
        if (str.equals("SELF_TYPE")) {
            return JetNodeTypes.SELF_TYPE;
        }
        if (str.equals("ANNOTATED_EXPRESSION")) {
            return JetNodeTypes.ANNOTATED_EXPRESSION;
        }
        if (str.equals("LONG_ANNOTATION")) {
            return JetNodeTypes.ANNOTATION;
        }
        if (str.equals("ANNOTATION_ENTRY")) {
            return JetNodeTypes.ANNOTATION_ENTRY;
        }
        if (str.equals("CLASS_BODY")) {
            return JetNodeTypes.CLASS_BODY;
        }
        if (str.equals("ENUM_CLASS_BODY")) {
            return JetNodeTypes.CLASS_BODY;
        }
        if (str.equals("ANONYMOUS_INITIALIZER")) {
            return JetNodeTypes.ANONYMOUS_INITIALIZER;
        }
        if (str.equals("VALUE_ARGUMENTS")) {
            return JetNodeTypes.VALUE_ARGUMENT_LIST;
        }
        if (str.equals("CALL_SUFFIX")) {
            return JetNodeTypes.CALL_EXPRESSION;
        }
        if (str.equals("DOT_IDENTIFIER")) {
            return JetNodeTypes.DOT_QUALIFIED_EXPRESSION;
        }
        if (str.equals("IMPORT_LIST")) {
            return JetNodeTypes.IMPORT_LIST;
        }
        if (str.equals("IMPORT_DIRECTIVE")) {
            return JetNodeTypes.IMPORT_DIRECTIVE;
        }
        if (str.equals("TYPE_PARAMETERS")) {
            return JetNodeTypes.TYPE_PARAMETER_LIST;
        }
        if (str.equals("TYPE_PARAMETER")) {
            return JetNodeTypes.TYPE_PARAMETER;
        }
        if (str.equals("FUNCTION_PARAMETER")) {
            return JetNodeTypes.VALUE_PARAMETER;
        }
        if (str.equals("TYPE")) {
            return JetNodeTypes.TYPE_REFERENCE;
        }
        if (str.equals("TYPE_FOLLOWED_BY_DOT")) {
            return JetNodeTypes.TYPE_REFERENCE;
        }
        if (str.equals("USER_TYPE")) {
            return JetNodeTypes.USER_TYPE;
        }
        if (str.equals("USER_TYPE_FOLLOWED_BY_DOT")) {
            return JetNodeTypes.USER_TYPE;
        }
        if (str.equals("VALUE_PARAMETERS")) {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str.equals("DELEGATION_SPECIFIER_EXT")) {
            return JetNodeTypes.DELEGATION_SPECIFIER_LIST;
        }
        if (str.equals("CONSTRUCTOR_CALLEE")) {
            return JetNodeTypes.CONSTRUCTOR_CALLEE;
        }
        if (str.equals("DELEGATOR_SUPER_CALL")) {
            return JetNodeTypes.DELEGATOR_SUPER_CALL;
        }
        if (str.equals("USER_TYPE_REFERENCE")) {
            return JetNodeTypes.TYPE_REFERENCE;
        }
        if (str.equals("VALUE_ARGUMENT_NAME")) {
            return JetNodeTypes.VALUE_ARGUMENT_NAME;
        }
        if (str.equals("VALUE_ARGUMENT")) {
            return JetNodeTypes.VALUE_ARGUMENT;
        }
        if (str.equals("EXPLICIT_DELEGATION")) {
            return JetNodeTypes.DELEGATOR_BY;
        }
        if (str.equals("DELEGATOR_SUPER_CLASS")) {
            return JetNodeTypes.DELEGATOR_SUPER_CLASS;
        }
        if (str.equals("DOT_QUALIFIED_EXPRESSION")) {
            return JetNodeTypes.DOT_QUALIFIED_EXPRESSION;
        }
        if (str.equals("GETTER")) {
            return JetNodeTypes.PROPERTY_ACCESSOR;
        }
        if (str.equals("SETTER")) {
            return JetNodeTypes.PROPERTY_ACCESSOR;
        }
        if (str.equals("SINGLE_VALUE_PARAMETER_LIST")) {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str.equals("PARAMETER")) {
            return JetNodeTypes.VALUE_PARAMETER;
        }
        if (str.equals("MODIFIER_LIST")) {
            return JetNodeTypes.MODIFIER_LIST;
        }
        if (str.equals("TYPE_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("TYPE_RHS_PLUS")) {
            return JetNodeTypes.BINARY_WITH_TYPE;
        }
        if (str.equals("THIS_EXPRESSION")) {
            return JetNodeTypes.THIS_EXPRESSION;
        }
        if (str.equals("THIS_REFERENCE")) {
            return JetNodeTypes.REFERENCE_EXPRESSION;
        }
        if (str.equals("TYPE_ARGUMENT_LIST")) {
            return JetNodeTypes.TYPE_ARGUMENT_LIST;
        }
        if (str.equals("TYPE_PROJECTION")) {
            return JetNodeTypes.TYPE_PROJECTION;
        }
        if (str.equals("NULLABLE_TYPE")) {
            return JetNodeTypes.NULLABLE_TYPE;
        }
        if (str.equals("WHEN_CONDITION_EXPRESSION")) {
            return JetNodeTypes.WHEN_CONDITION_EXPRESSION;
        }
        if (str.equals("WHEN_CONDITION_IN_RANGE")) {
            return JetNodeTypes.WHEN_CONDITION_IN_RANGE;
        }
        if (str.equals("WHEN_CONDITION_IS_PATTERN")) {
            return JetNodeTypes.WHEN_CONDITION_IS_PATTERN;
        }
        if (str.equals("PRIMARY_CONSTRUCTOR_MODIFIER_LIST")) {
            return JetNodeTypes.PRIMARY_CONSTRUCTOR_MODIFIER_LIST;
        }
        if (str.equals("ENUM_ENTRY")) {
            return JetNodeTypes.ENUM_ENTRY;
        }
        if (str.equals("OBJECT_DECLARATION_NAME")) {
            return JetNodeTypes.OBJECT_DECLARATION_NAME;
        }
        if (str.equals("INITIALIZER_EXT")) {
            return JetNodeTypes.INITIALIZER_LIST;
        }
        if (str.equals("PREFIX_OPERATION_EXPRESSION")) {
            return JetNodeTypes.PREFIX_EXPRESSION;
        }
        if (str.equals("LABEL_EXPRESSION")) {
            return JetNodeTypes.LABELED_EXPRESSION;
        }
        if (str.equals("PREFIX_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("TYPE_ARGUMENTS")) {
            return JetNodeTypes.TYPE_ARGUMENT_LIST;
        }
        if (str.equals("TYPE_LIST")) {
            return JetNodeTypes.TYPE_PROJECTION;
        }
        if (str.equals("JUMP_THROW")) {
            return JetNodeTypes.THROW;
        }
        if (str.equals("JUMP_RETURN")) {
            return JetNodeTypes.RETURN;
        }
        if (str.equals("JUMP_CONTINUE")) {
            return JetNodeTypes.CONTINUE;
        }
        if (str.equals("JUMP_BREAK")) {
            return JetNodeTypes.BREAK;
        }
        if (str.equals("FLOAT_CONSTANT")) {
            return JetNodeTypes.FLOAT_CONSTANT;
        }
        if (str.equals("PARENTHESIZED_EXPRESSION")) {
            return JetNodeTypes.PARENTHESIZED;
        }
        if (str.equals("FUNCTION_LITERAL_EXPRESSION")) {
            return JetNodeTypes.FUNCTION_LITERAL_EXPRESSION;
        }
        if (str.equals("STATEMENTS_BLOCK")) {
            return JetNodeTypes.BLOCK;
        }
        if (str.equals("INDICES")) {
            return JetNodeTypes.INDICES;
        }
        if (str.equals("IN_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("NAMED_INFIX_PLUS")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("NAMED_INFIX_FIRST")) {
            return JetNodeTypes.IS_EXPRESSION;
        }
        if (str.equals("IS_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("STRING_TEMPLATE")) {
            return JetNodeTypes.STRING_TEMPLATE;
        }
        if (str.equals("SAFE_ACCESS_EXPRESSION")) {
            return JetNodeTypes.SAFE_ACCESS_EXPRESSION;
        }
        if (str.equals("ELVIS_ACCESS_EXPRESSION")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("ELVIS_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("DISJUNCTION_PLUS")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("OR_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("BINARY_CONSTANT")) {
            return JetNodeTypes.BOOLEAN_CONSTANT;
        }
        if (str.equals("AND_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("CONJUNCTION_PLUS")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("OBJECT_DECLARATION")) {
            return JetNodeTypes.OBJECT_DECLARATION;
        }
        if (str.equals("OBJECT_LITERAL")) {
            return JetNodeTypes.OBJECT_LITERAL;
        }
        if (str.equals("TYPEDEF")) {
            return JetNodeTypes.TYPEDEF;
        }
        if (str.equals("TYPE_CONSTRAINT_EXT")) {
            return JetNodeTypes.TYPE_CONSTRAINT_LIST;
        }
        if (str.equals("TYPE_CONSTRAINT")) {
            return JetNodeTypes.TYPE_CONSTRAINT;
        }
        if (str.equals("MODIFIER_LIST_FOLLOWED_BY_IDENTIFIER")) {
            return JetNodeTypes.MODIFIER_LIST;
        }
        if (str.equals("ANNOTATION_WITH_SHORT")) {
            return JetNodeTypes.ANNOTATION;
        }
        if (str.equals("CONDITION")) {
            return JetNodeTypes.CONDITION;
        }
        if (str.equals("THEN_EXPRESSION")) {
            return JetNodeTypes.THEN;
        }
        if (str.equals("ELSE_EXPRESSION")) {
            return JetNodeTypes.ELSE;
        }
        if (str.equals("NULL")) {
            return JetNodeTypes.NULL;
        }
        if (str.equals("VALUE_PARAMETER_LIST")) {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str.equals("FUNCTION_TYPE")) {
            return JetNodeTypes.FUNCTION_TYPE;
        }
        if (str.equals("PARAMETER_OR_MODIFIER_TYPE")) {
            return JetNodeTypes.VALUE_PARAMETER;
        }
        if (str.equals("FUNCTION_TYPE_RECEIVER")) {
            return JetNodeTypes.TYPE_REFERENCE;
        }
        if (str.equals("FUNCTION_TYPE_RECEIVER_REFERENCE")) {
            return JetNodeTypes.FUNCTION_TYPE_RECEIVER;
        }
        if (str.equals("SIMPLE_USER_TYPE_ADD")) {
            return JetNodeTypes.USER_TYPE;
        }
        if (str.equals("LEFT_FUNCTION_TYPE")) {
            return JetNodeTypes.FUNCTION_TYPE;
        }
        if (str.equals("SIMPLE_USER_TYPE")) {
            return JetNodeTypes.USER_TYPE;
        }
        if (str.equals("PLUS_PLUS_AND_OTHERS_EXPRESSION")) {
            return JetNodeTypes.POSTFIX_EXPRESSION;
        }
        if (str.equals("PLUS_PLUS_AND_OTHERS_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("EQUALITY_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("EQUALITY_COMPARISON_EXPRESSION")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("ASTERISK")) {
            return JetNodeTypes.TYPE_PROJECTION;
        }
        if (str.equals("CLASS_OBJECT")) {
            return JetNodeTypes.CLASS_OBJECT;
        }
        if (str.equals("OBJECT_UNNAMED")) {
            return JetNodeTypes.OBJECT_DECLARATION;
        }
        if (str.equals("MULTIPLICATIVE_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("MULTIPLICATIVE_EXPRESSION_PLUS")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("FUNCTION_LITERAL_ARGUMENT")) {
            return JetNodeTypes.FUNCTION_LITERAL_ARGUMENT;
        }
        if (str.equals("FUNCTION_LITERAL_VALUE_PARAMETER_LIST_1")) {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str.equals("FUNCTION_LITERAL_VALUE_PARAMETER_LIST_2")) {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str.equals("OBJECT")) {
            return JetNodeTypes.OBJECT_DECLARATION;
        }
        if (str.equals("OBJECT_NAME")) {
            return JetNodeTypes.OBJECT_DECLARATION_NAME;
        }
        if (str.equals("COMPARISON_EXPRESSION")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("COMPARISON_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("CHARACTER_CONSTANT")) {
            return JetNodeTypes.CHARACTER_CONSTANT;
        }
        if (str.equals("SUPER_REFERENCE")) {
            return JetNodeTypes.REFERENCE_EXPRESSION;
        }
        if (str.equals("SUPER_EXPRESSION")) {
            return JetNodeTypes.SUPER_EXPRESSION;
        }
        if (str.equals("LABEL")) {
            return JetNodeTypes.LABEL_QUALIFIER;
        }
        if (str.equals("LABEL_LABEL")) {
            return JetNodeTypes.LABEL;
        }
        if (str.equals("WHEN_PROPERTY")) {
            return JetNodeTypes.PROPERTY;
        }
        if (str.equals("TRY_BLOCK")) {
            return JetNodeTypes.TRY;
        }
        if (str.equals("SINGLE_VALUE_PARAMETER_LIST_WITH_BRACKETS")) {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str.equals("FINALLY_BLOCK")) {
            return JetNodeTypes.FINALLY;
        }
        if (str.equals("FOR_VALUE_PARAMETER")) {
            return JetNodeTypes.VALUE_PARAMETER;
        }
        if (str.equals("LOOP_RANGE")) {
            return JetNodeTypes.LOOP_RANGE;
        }
        if (str.equals("BODY")) {
            return JetNodeTypes.BODY;
        }
        if (str.equals("THEN_EXPRESSION_WITH_SEMI")) {
            return JetNodeTypes.THEN;
        }
        if (str.equals("PROPERTY_DELEGATE")) {
            return JetNodeTypes.PROPERTY_DELEGATE;
        }
        if (str.equals("RANGE_EXPRESSION_PLUS")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("RANGE_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("CLASS_DECLARATION_LOCAL")) {
            return JetNodeTypes.CLASS;
        }
        if (str.equals("INFIX_FUNCTION_CALL_PLUS")) {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str.equals("SIMPLE_NAME_OPERATION")) {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str.equals("SHORT_TEMPLATE_ENTRY")) {
            return JetNodeTypes.SHORT_STRING_TEMPLATE_ENTRY;
        }
        if (str.equals("LITERAL_STRING_TEMPLATE_ENTRY")) {
            return JetNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY;
        }
        if (str.equals("VALUE_PARAMETER_WITH_TYPE")) {
            return JetNodeTypes.VALUE_PARAMETER;
        }
        if (str.equals("VALUE_PARAMETER_NO_TYPE")) {
            return JetNodeTypes.VALUE_PARAMETER;
        }
        if (str.equals("SCRIPT")) {
            return JetNodeTypes.SCRIPT;
        }
        if (str.equals("FIELD_IDENTIFIER_REFERENCE")) {
            return JetNodeTypes.REFERENCE_EXPRESSION;
        }

        return new IElementType(str, JetLanguage.INSTANCE);
    }

    public static final IFileElementType KT_FILE_TYPE = new IFileElementType("KT_FILE_TYPE", JetLanguage.INSTANCE);

    @Override
    public IFileElementType getFileNodeType() {
        //return JetStubElementTypes.FILE;
        return KT_FILE_TYPE;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return JetTokens.WHITESPACES;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return JetTokens.COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return JetTokens.STRINGS;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode astNode) {
        IElementType elementType = astNode.getElementType();

        if (elementType instanceof JetStubElementType) {
            return ((JetStubElementType) elementType).createPsiFromAst(astNode);
        }
        else if (elementType == JetNodeTypes.TYPE_CODE_FRAGMENT ||
                 elementType == JetNodeTypes.EXPRESSION_CODE_FRAGMENT  ||
                 elementType == JetNodeTypes.BLOCK_CODE_FRAGMENT) {
            return new ASTWrapperPsiElement(astNode);
        }
        else if (elementType instanceof JetNodeType) {
            return ((JetNodeType) elementType).createPsi(astNode);
        }
        return KotlinTypes.Factory.createElement(astNode);
    }

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new JetLexer();
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new JetFile(fileViewProvider, false);
        /*return new PsiFileBase(fileViewProvider, JetLanguage.INSTANCE) {

            @NotNull
            @Override
            public FileType getFileType() {
                return FILE_TYPE;
            }

            @Override
            public void accept(@NotNull PsiElementVisitor visitor) {
                visitor.visitFile(this);
            }
        };*/
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        return SpaceRequirements.MAY;
    }

    @Override
    public PsiParser createParser(Project project) {
        return new KotlinParser();
    }


    public static IElementType createTokenType(String text) {
        return new IElementType(text, JetLanguage.INSTANCE);
    }
}