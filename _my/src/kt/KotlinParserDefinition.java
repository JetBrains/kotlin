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
        if (str == "ADDITIVE_OPERATION") {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str == "ASSIGNMENT_EXPRESSION") {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str == "ADDITIVE_EXPRESSION_PLUS") {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str == "ASSIGNMENT_OPERATOR") {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str == "VALUE_PARAMETERS") {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str == "FUNCTION") {
            return JetNodeTypes.FUN;
        }
        if (str == "FUNCTION_LITERAL") {
            return JetNodeTypes.FUNCTION_LITERAL;
        }
        if (str == "PACKAGE_HEADER") {
            return JetNodeTypes.PACKAGE_DIRECTIVE;
        }
        if (str == "BLOCK") {
            return JetNodeTypes.BLOCK;
        }
        if (str == "REFERENCE_EXPRESSION") {
            return JetNodeTypes.REFERENCE_EXPRESSION;
        }
        if (str == "INTEGER_CONSTANT") {
            return JetNodeTypes.INTEGER_CONSTANT;
        }
        if (str == "PROPERTY") {
            return JetNodeTypes.PROPERTY;
        }
        if (str == "PROPERTY_LOCAL") {
            return JetNodeTypes.PROPERTY;
        }
        if (str == "CLASS_DECLARATION") {
            return JetNodeTypes.CLASS;
        }
        if (str == "DO_WHILE_LOOP") {
            return JetNodeTypes.DO_WHILE;
        }
        if (str == "FOR_LOOP") {
            return JetNodeTypes.FOR;
        }
        if (str == "ARRAY_ACCESS") {
            return JetNodeTypes.ARRAY_ACCESS_EXPRESSION;
        }
        if (str == "IF_EXPRESSION") {
            return JetNodeTypes.IF;
        }
        if (str == "CATCH_BLOCK") {
            return JetNodeTypes.CATCH;
        }
        if (str == "WHILE_LOOP") {
            return JetNodeTypes.WHILE;
        }
        if (str == "WHEN") {
            return JetNodeTypes.WHEN;
        }
        if (str == "WHEN_ENTRY") {
            return JetNodeTypes.WHEN_ENTRY;
        }
        if (str == "SELF_TYPE") {
            return JetNodeTypes.SELF_TYPE;
        }
        if (str == "ANNOTATED_EXPRESSION") {
            return JetNodeTypes.ANNOTATED_EXPRESSION;
        }
        if (str == "ANNOTATION") {
            return JetNodeTypes.ANNOTATION;
        }
        if (str == "ANNOTATION_ENTRY") {
            return JetNodeTypes.ANNOTATION_ENTRY;
        }
        if (str == "CLASS_BODY") {
            return JetNodeTypes.CLASS_BODY;
        }
        if (str == "CLASS_BODY_NO_Q") {
            return JetNodeTypes.CLASS_BODY;
        }
        if (str == "ENUM_CLASS_BODY") {
            return JetNodeTypes.CLASS_BODY;
        }
        if (str == "ANONYMOUS_INITIALIZER") {
            return JetNodeTypes.ANONYMOUS_INITIALIZER;
        }
        if (str == "VALUE_ARGUMENTS") {
            return JetNodeTypes.VALUE_ARGUMENT_LIST;
        }
        if (str == "CALL_SUFFIX") {
            return JetNodeTypes.CALL_EXPRESSION;
        }
        if (str == "DOT_IDENTIFIER") {
            return JetNodeTypes.DOT_QUALIFIED_EXPRESSION;
        }
        if (str == "IMPORT_LIST") {
            return JetNodeTypes.IMPORT_LIST;
        }
        if (str == "IMPORT_DIRECTIVE") {
            return JetNodeTypes.IMPORT_DIRECTIVE;
        }
        if (str == "TYPE_PARAMETERS") {
            return JetNodeTypes.TYPE_PARAMETER_LIST;
        }
        if (str == "TYPE_PARAMETER") {
            return JetNodeTypes.TYPE_PARAMETER;
        }
        if (str == "FUNCTION_PARAMETER") {
            return JetNodeTypes.VALUE_PARAMETER;
        }
        if (str == "TYPE") {
            return JetNodeTypes.TYPE_REFERENCE;
        }
        if (str == "TYPE_FOLLOWED_BY_DOT") {
            return JetNodeTypes.TYPE_REFERENCE;
        }
        if (str == "USER_TYPE") {
            return JetNodeTypes.USER_TYPE;
        }
        if (str == "USER_TYPE_FOLLOWED_BY_DOT") {
            return JetNodeTypes.USER_TYPE;
        }
        if (str == "VALUE_PARAMETERS") {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str == "DELEGATION_SPECIFIER_EXT") {
            return JetNodeTypes.DELEGATION_SPECIFIER_LIST;
        }
        if (str == "CONSTRUCTOR_CALLEE") {
            return JetNodeTypes.CONSTRUCTOR_CALLEE;
        }
        if (str == "DELEGATOR_SUPER_CALL") {
            return JetNodeTypes.DELEGATOR_SUPER_CALL;
        }
        if (str == "USER_TYPE_REFERENCE") {
            return JetNodeTypes.TYPE_REFERENCE;
        }
        if (str == "VALUE_ARGUMENT_NAME") {
            return JetNodeTypes.VALUE_ARGUMENT_NAME;
        }
        if (str == "VALUE_ARGUMENT") {
            return JetNodeTypes.VALUE_ARGUMENT;
        }
        if (str == "EXPLICIT_DELEGATION") {
            return JetNodeTypes.DELEGATOR_BY;
        }
        if (str == "DELEGATOR_SUPER_CLASS") {
            return JetNodeTypes.DELEGATOR_SUPER_CLASS;
        }
        if (str == "DOT_QUALIFIED_EXPRESSION") {
            return JetNodeTypes.DOT_QUALIFIED_EXPRESSION;
        }
        if (str == "GETTER") {
            return JetNodeTypes.PROPERTY_ACCESSOR;
        }
        if (str == "SETTER") {
            return JetNodeTypes.PROPERTY_ACCESSOR;
        }
        if (str == "SINGLE_VALUE_PARAMETER_LIST") {
            return JetNodeTypes.VALUE_PARAMETER_LIST;
        }
        if (str == "PARAMETER") {
            return JetNodeTypes.VALUE_PARAMETER;
        }
        if (str == "MODIFIERS_PLUS") {
            return JetNodeTypes.MODIFIER_LIST;
        }
        if (str == "TYPE_OPERATION") {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str == "TYPE_RHS_PLUS") {
            return JetNodeTypes.BINARY_WITH_TYPE;
        }
        if (str == "THIS_EXPRESSION") {
            return JetNodeTypes.THIS_EXPRESSION;
        }
        if (str == "THIS_REFERENCE") {
            return JetNodeTypes.REFERENCE_EXPRESSION;
        }
        if (str == "TYPE_ARGUMENT_LIST") {
            return JetNodeTypes.TYPE_ARGUMENT_LIST;
        }
        if (str == "TYPE_PROJECTION") {
            return JetNodeTypes.TYPE_PROJECTION;
        }
        if (str == "NULLABLE_TYPE") {
            return JetNodeTypes.NULLABLE_TYPE;
        }
        if (str == "WHEN_CONDITION_EXPRESSION") {
            return JetNodeTypes.WHEN_CONDITION_EXPRESSION;
        }
        if (str == "WHEN_CONDITION_IN_RANGE") {
            return JetNodeTypes.WHEN_CONDITION_IN_RANGE;
        }
        if (str == "WHEN_CONDITION_IS_PATTERN") {
            return JetNodeTypes.WHEN_CONDITION_IS_PATTERN;
        }
        if (str == "PRIMARY_CONSTRUCTOR_MODIFIER_LIST") {
            return JetNodeTypes.PRIMARY_CONSTRUCTOR_MODIFIER_LIST;
        }
        if (str == "ENUM_ENTRY") {
            return JetNodeTypes.ENUM_ENTRY;
        }
        if (str == "OBJECT_DECLARATION_NAME") {
            return JetNodeTypes.OBJECT_DECLARATION_NAME;
        }
        if (str == "INITIALIZER_EXT") {
            return JetNodeTypes.INITIALIZER_LIST;
        }
        if (str == "PREFIX_UNARY_OPERATION") {
            return JetNodeTypes.PREFIX_EXPRESSION;
        }
        if (str == "PREFIX_OPERATION") {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str == "TYPE_ARGUMENTS") {
            return JetNodeTypes.TYPE_ARGUMENT_LIST;
        }
        if (str == "TYPE_LIST") {
            return JetNodeTypes.TYPE_PROJECTION;
        }
        if (str == "JUMP_THROW") {
            return JetNodeTypes.THROW;
        }
        if (str == "JUMP_RETURN") {
            return JetNodeTypes.RETURN;
        }
        if (str == "JUMP_CONTINUE") {
            return JetNodeTypes.CONTINUE;
        }
        if (str == "JUMP_BREAK") {
            return JetNodeTypes.BREAK;
        }
        if (str == "FLOAT_CONSTANT") {
            return JetNodeTypes.FLOAT_CONSTANT;
        }
        if (str == "PARENTHESIZED_EXPRESSION") {
            return JetNodeTypes.PARENTHESIZED;
        }
        if (str == "FUNCTION_LITERAL_EXPRESSION") {
            return JetNodeTypes.FUNCTION_LITERAL_EXPRESSION;
        }
        if (str == "STATEMENTS_BLOCK") {
            return JetNodeTypes.BLOCK;
        }
        if (str == "INDICES") {
            return JetNodeTypes.INDICES;
        }
        if (str == "IN_OPERATION") {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str == "NAMED_INFIX_PLUS") {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str == "NAMED_INFIX_FIRST") {
            return JetNodeTypes.IS_EXPRESSION;
        }
        if (str == "IS_OPERATION") {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str == "STRING_TEMPLATE") {
            return JetNodeTypes.STRING_TEMPLATE;
        }
        if (str == "SAFE_ACCESS_EXPRESSION") {
            return JetNodeTypes.SAFE_ACCESS_EXPRESSION;
        }
        if (str == "ELVIS_ACCESS_EXPRESSION") {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str == "ELVIS_OPERATION") {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str == "DISJUNCTION_PLUS") {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str == "OR_OPERATION") {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str == "BINARY_CONSTANT") {
            return JetNodeTypes.BOOLEAN_CONSTANT;
        }
        if (str == "AND_OPERATION") {
            return JetNodeTypes.OPERATION_REFERENCE;
        }
        if (str == "CONJUNCTION_PLUS") {
            return JetNodeTypes.BINARY_EXPRESSION;
        }
        if (str == "OBJECT_DECLARATION") {
            return JetNodeTypes.OBJECT_DECLARATION;
        }
        if (str == "OBJECT_LITERAL") {
            return JetNodeTypes.OBJECT_LITERAL;
        }
        if (str == "TYPEDEF") {
            return JetNodeTypes.TYPEDEF;
        }
        if (str == "TYPE_CONSTRAINT_EXT") {
            return JetNodeTypes.TYPE_CONSTRAINT_LIST;
        }
        if (str == "TYPE_CONSTRAINT") {
            return JetNodeTypes.TYPE_CONSTRAINT;
        }
        if (str == "MODIFIERS_PLUS_FOLLOWED_BY_IDENTIFIER") {
            return JetNodeTypes.MODIFIER_LIST;
        }
        if (str == "ANNOTATION_WITH_SHORT") {
            return JetNodeTypes.ANNOTATION;
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