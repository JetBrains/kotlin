/*
 * @author max
 */
package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.JetNodeType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetParsing extends AbstractJetParsing {
    private static final Map<String, IElementType> MODIFIER_KEYWORD_MAP = new HashMap<String, IElementType>();
    static {
        for (IElementType softKeyword : MODIFIER_KEYWORDS.getTypes()) {
            MODIFIER_KEYWORD_MAP.put(((JetKeywordToken) softKeyword).getValue(), softKeyword);
        }
    }

    private static final TokenSet TOPLEVEL_OBJECT_FIRST = TokenSet.create(TYPE_KEYWORD, CLASS_KEYWORD,
                EXTENSION_KEYWORD, FUN_KEYWORD, VAL_KEYWORD, REF_KEYWORD, NAMESPACE_KEYWORD, DECOMPOSER_KEYWORD);

    private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, WRAPS_KEYWORD, LPAR, COLON, LBRACE), TOPLEVEL_OBJECT_FIRST);
    private static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, WRAPS_KEYWORD, LPAR, COLON, LBRACE, GT);
    private static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR);
    private static final TokenSet NAMESPACE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON);
    private static final TokenSet TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LBRACE, LPAR);

    private final JetExpressionParsing myExpressionParsing;

    public JetParsing(SemanticWitespaceAwarePsiBuilder builder) {
        super(builder);
        this.myExpressionParsing = new JetExpressionParsing(builder);
    }

    /*
     * [start] jetlFile
     *   : preamble toplevelObject[| import]* [eof]
     *   ;
     */
    public void parseFile() {
        PsiBuilder.Marker fileMarker = mark();
        PsiBuilder.Marker namespaceMarker = mark();

        parsePreamble();

        parseToplevelDeclarations(false);

        namespaceMarker.done(NAMESPACE);
        fileMarker.done(JET_FILE);
    }

    /*
     * toplevelObject[| import]*
     */
    private void parseToplevelDeclarations(boolean insideBlock) {
        while (!eof() && (!insideBlock || !at(RBRACE))) {
            if (at(IMPORT_KEYWORD)) {
                parseImportDirective();
            }
            else {
                parseTopLevelObject();
            }
        }
    }

    /*
     *preamble
     *  : namespaceHeader? import*
     *  ;
     */
    private void parsePreamble() {
        /*
         * namespaceHeader
         *   : "namespace" SimpleName{"."}
         *   ;
         */
        if (at(NAMESPACE_KEYWORD)) {
            advance(); // NAMESPACE_KEYWORD

            parseNamespaceName();
        }

        while (at(IMPORT_KEYWORD)) {
            parseImportDirective();
        }
    }

    /* SimpleName{"."} */
    private void parseNamespaceName() {
        PsiBuilder.Marker nsName = mark();
        expect(IDENTIFIER, "Expecting qualified name", NAMESPACE_NAME_RECOVERY_SET);
        while (at(DOT)) {
            advance(); // DOT
            expect(IDENTIFIER, "Namespace name must be a '.'-separated identifier list", NAMESPACE_NAME_RECOVERY_SET);
            if (at(EOL_OR_SEMICOLON)) break;
        }
        nsName.done(NAMESPACE_NAME);
    }

    /*
     * import
     *   : "import" SimpleName{"."} ("." "*" | "as" SimpleName)?
     *   ;
     */
    private void parseImportDirective() {
        assert at(IMPORT_KEYWORD);
        PsiBuilder.Marker importDirective = mark();
        advance(); // IMPORT_KEYWORD

        expect(IDENTIFIER, "Expecting qualified name", TokenSet.create(DOT, MAP));
        while (at(DOT)) {
            advance(); // DOT
            if (at(IDENTIFIER)) {
                advance(); // IDENTIFIER
            }
            else if (at(MUL)) {
                advance(); // MUL
                handleUselessRename();
                break;
            }
            else {
                errorWithRecovery("Qualified name must be a '.'-separated identifier list", TokenSet.create(AS_KEYWORD, DOT, MAP, SEMICOLON));
            }
        }
        if (at(MAP)) {
            advance(); // MAP
            handleUselessRename();
        }
        else if (at(AS_KEYWORD)) {
            advance(); // AS_KEYWORD
            expect(IDENTIFIER, "Expecting identifier", TokenSet.create(SEMICOLON));
        }
        consumeIf(SEMICOLON);
        importDirective.done(IMPORT_DIRECTIVE);
    }

    private void handleUselessRename() {
        if (at(AS_KEYWORD)) {
            PsiBuilder.Marker as = mark();
            advance(); // AS_KEYWORD
            consumeIf(IDENTIFIER);
            as.error("Cannot rename a all imported items to one identifier");
        }
    }

    /*
     * toplevelObject
     *   : namespace
     *   : class
     *   : extension
     *   : function
     *   : property
     *   : typedef
     *   ;
     */
    private void parseTopLevelObject() {
        PsiBuilder.Marker decl = mark();
        parseModifierList();

        JetToken keywordToken = tt();
        JetNodeType declType = null;
        if (keywordToken == NAMESPACE_KEYWORD) {
            declType = parseNamespaceBlock();
        }
        else if (keywordToken == CLASS_KEYWORD) {
            declType = parseClass();
        }
        else if (keywordToken == EXTENSION_KEYWORD) {
            declType = parseExtension();
        }
        else if (keywordToken == FUN_KEYWORD) {
            declType = parseFunction();
        }
        else if (keywordToken == VAL_KEYWORD || keywordToken == VAR_KEYWORD) {
            declType = parseProperty();
        }
        else if (keywordToken == TYPE_KEYWORD) {
            declType = parseTypedef();
        }
        else if (keywordToken == DECOMPOSER_KEYWORD) {
            declType = parseDecomposer();
        }

        if (declType == null) {
            errorAndAdvance("Expecting namespace or top level declaration");
            decl.drop();
        }
        else {
            decl.done(declType);
        }
    }

    /*
     * decomposer
     *   : attributes "decomposer" (type ".")? SimpleName "(" (attributes SimpleName){","} ")" // Public properties only
     *   ;
     */
    private JetNodeType parseDecomposer() {
        // TODO
        return DECOMPOSER;
    }

    /*
     * modifier
     *   : "virtual"
     *   : "enum"
     *   : "open"
     *   : "attribute"
     *   : "override"
     *   : "abstract"
     *   : "private"
     *   : "protected"
     *   : "public"
     *   : "internal"
     *   : "lazy"
     */
    private boolean parseModifierSoftKeyword() {
        if (!at(IDENTIFIER)) return false;
        String tokenText = myBuilder.getTokenText();
        IElementType tokenType = MODIFIER_KEYWORD_MAP.get(tokenText);
        if (tokenType != null) {
            myBuilder.remapCurrentToken(tokenType);
            advance();
            return true;
        }
        return false;
    }

    /*
     * (modifier | attribute)*
     */
    private void parseModifierList() {
        PsiBuilder.Marker list = mark();
        boolean empty = true;
        while (!eof()) {
            if (at(LBRACKET)) {
                parseAttributeAnnotation();
            } else if (MODIFIER_KEYWORDS.contains(tt())) {
                advance();
            }
            else {
                if (!parseModifierSoftKeyword()) break;
            }
            empty = false;
        }
        if (empty) {
            list.drop();
        } else {
            list.done(MODIFIER_LIST);
        }
    }

    /*
     * attributeAnnotation
     *   : "[" attribute{","} "]"
     *   ;
     */
    private void parseAttributeAnnotation() {
        assert at(LBRACKET);
        PsiBuilder.Marker annotation = mark();

        advance(); // LBRACKET

        while (true) {
            if (at(IDENTIFIER)) {
                parseAttribute();
                if (!at(COMMA)) break;
                advance(); // COMMA
            }
            else {
                error("Expecting a comma-separated list of attributes");
                break;
            }
        }

        expect(RBRACKET, "Expecting ']' to close an attribute annotation");

        annotation.done(ATTRIBUTE_ANNOTATION);
    }

    /*
     * attribute
     *   // : SimpleName{"."} valueArguments?
     *   [for recovery: userType valueArguments?]
     *   ;
     */
    private void parseAttribute() {
        PsiBuilder.Marker attribute = mark();
        parseUserType();
        if (at(LPAR)) parseValueArgumentList();
        attribute.done(ATTRIBUTE);
    }

    /*
     * valueArguments
     *   : "(" (SimpleName "=")? ("out" | "ref")? expression{","} ")"
     *   ;
     */
    private void parseValueArgumentList() {
        assert at(LPAR);

        PsiBuilder.Marker list = mark();

        advance(); // LPAR

        if (!at(RPAR)) {
            while (true) {
                parseValueArgument();
                if (!at(COMMA)) break;
                advance(); // COMMA
            }
        }

        expect(RPAR, "Expecting ')'");

        list.done(VALUE_ARGUMENT_LIST);
    }

    /*
     * (SimpleName "=")? ("out" | "ref")? expression
     */
    private void parseValueArgument() {
        PsiBuilder.Marker argument = mark();
        JetNodeType type = VALUE_ARGUMENT;
        if (at(IDENTIFIER) && lookahead(1) == EQ) {
            advance(); // IDENTIFIER
            advance(); // EQ
            type = NAMED_ARGUMENT;
        }
        if (at(OUT_KEYWORD) || at(REF_KEYWORD)) advance(); // REF or OUT
        myExpressionParsing.parseExpression();
        argument.done(type);
    }

    /*
     * namespace
     *   : "namespace" SimpleName{"."} "{"
     *        import*
     *        toplevelObject[| import]*
     *     "}"
     *   ;
     */
    private JetNodeType parseNamespaceBlock() {
        assert at(NAMESPACE);
        advance(); // NAMESPACE

        if (at(LBRACE)) {
            error("Expecting namespace name");
        }
        else {
            parseNamespaceName();
        }

        if (!at(LBRACE)) {
            error("A namespace block in '{...}' expected");
            return NAMESPACE;
        }

        advance(); // LBRACE
        PsiBuilder.Marker namespaceBody = mark();

        parseToplevelDeclarations(true);

        namespaceBody.done(NAMESPACE_BODY);
        expect(RBRACE, "Expecting '}'");

        return NAMESPACE;
    }

    /*
     * class
     *   : modifiers "class" SimpleName
     *       typeParameters?
     *       "wraps"?
     *       ("(" primaryConstructorParameter{","} ")")?
     *       (":" attributes delegationSpecifier{","})?
     *       classBody?
     *   ;
     */
    private JetNodeType parseClass() {
        assert at(CLASS_KEYWORD);
        advance(); // CLASS_KEYWORD

        expect(IDENTIFIER, "Class name expected", CLASS_NAME_RECOVERY_SET);
        parseTypeParameterList();

        consumeIf(WRAPS_KEYWORD);
        if (at(LPAR)) {
            parsePrimaryConstructorParameterList();
        }

        if (at(COLON)) {
            advance(); // COLON
            parseDelegationSpecifierList();
        }

        if (at(LBRACE)) {
            parseClassBody();
        }

        return CLASS;
    }

    /*
     * classBody
     *   : ("{" memberDeclaration{","} "}")?
     *   ;
     */
    private void parseClassBody() {
        assert at(LBRACE);
        PsiBuilder.Marker body = mark();
        advance(); // LBRACE

        while (!eof()) {
            if (at(RBRACE)) {
                break;
            }
            advance(); // TODO
        }
        expect(RBRACE, "Missing '}");
        body.done(CLASS_BODY);
    }

    /*
     * typedef
     *   : modifiers "type" SimpleName typeParameters? "=" type
     *   ;
     */
    private JetNodeType parseTypedef() {
        assert at(TYPE_KEYWORD);

        advance(); // TYPE_KEYWORD

        expect(IDENTIFIER, "Type name expected", TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOPLEVEL_OBJECT_FIRST));

        parseTypeParameterList();

        expect(EQ, "Expecting '='", TokenSet.orSet(TOPLEVEL_OBJECT_FIRST, TokenSet.create(SEMICOLON)));

        parseTypeRef();

        consumeIf(SEMICOLON);

        return TYPEDEF;
    }

    /*
     * property
     *   : modifiers ("val" | "var") (type ".")? propertyRest
     *   ;
     */
    private JetNodeType parseProperty() {
        advance(); // TODO
        return PROPERTY;
    }

    /*
     * function
     *   : modifiers "fun" (type ".")? functionRest
     *   ;
     *
     * functionRest
     *   : attributes SimpleName typeParameters? functionParameters (":" type)? functionBody?
     *   ;
     */
    private JetNodeType parseFunction() {
        advance(); // TODO
        return FUN;
    }

    /*
     * extension
     *   : modifiers "extension" SimpleName? typeParameters? "for" type classBody? // properties cannot be lazy, cannot have backing fields
     *   ;
     */
    private JetNodeType parseExtension() {
        advance(); // TODO
        return EXTENSION;
    }

    /*
     * delegationSpecifier{","}
     */
    private void parseDelegationSpecifierList() {
        PsiBuilder.Marker list = mark();

        while (true) {
            if (at(COMMA)) {
                errorAndAdvance("Expecting a delegation specifier");
                continue;
            }
            parseDelegationSpecifier();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        list.done(DELEGATION_SPECIFIER_LIST);
    }

    /*
     * attributes delegationSpecifier
     *
     * delegationSpecifier
     *   : constructorInvocation // type and constructor arguments
     *   : explicitDelegation
     *   ;
     *
     * explicitDelegation
     *   : userType "by" expression
     *   ;
     */
    private void parseDelegationSpecifier() {
        PsiBuilder.Marker specifier = mark();
        parseAttributeList();

        PsiBuilder.Marker delegator = mark();
        if (at(LPAR)) {
            error("Expecting type name");
        }
        else {
            parseTypeRef();
        }

        if (at(BY_KEYWORD)) {
            advance(); // BY_KEYWORD
            myExpressionParsing.parseExpression();
            delegator.done(DELEGATOR_BY);
        }
        else if (at(LPAR)) {
            parseValueArgumentList();
            delegator.done(DELEGATOR_SUPER_CALL);
        }
        else {
            delegator.done(DELEGATOR_SUPER_CLASS);
        }

        specifier.done(DELEGATION_SPECIFIER);
    }


    /*
     * attributes
     *   : attributeAnnotation*
     *   ;
     */
    private void parseAttributeList() {
        while (at(LBRACKET)) parseAttributeAnnotation();
    }

    /*
     * ("(" primaryConstructorParameter{","} ")")
     */
    private void parsePrimaryConstructorParameterList() {
        assert at(LPAR);
        PsiBuilder.Marker cons = mark();
        advance(); // LPAR

        while (true) {
            parsePrimaryConstructorParameter();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        expect(RPAR, "')' expected");

        cons.done(PRIMARY_CONSTRUCTOR_PARAMETERS_LIST);
    }

    /*
     * primaryConstructorParameter
     *   : modifiers ("val" | "var")? functionParameterRest
     *   ;
     */
    private void parsePrimaryConstructorParameter() {
        PsiBuilder.Marker param = mark();
        parseModifierList();

        if (at(VAR_KEYWORD) || at(VAL_KEYWORD)) {
            advance(); // VAR_KEYWORD | VAL_KEYWORD
        }

        parseFunctionParameterRest();

        param.done(PRIMARY_CONSTRUCTOR_PARAMETER);
    }

    /*
     * functionParameterRest
     *   : parameter ("=" expression)?
     *   ;
     */
    private boolean parseFunctionParameterRest() {
        expect(IDENTIFIER, "Parameter name expected", PARAMETER_NAME_RECOVERY_SET);

        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }
        else {
            error("Parameters must have type annotation");
            return false;
        }

        if (at(EQ)) {
            advance(); // EQ
            myExpressionParsing.parseExpression();
        }
        return true;
    }

    /*
     * typeParameters
     *   : ("<" typeParameter{","} ">"
     *      ("where" typeConstraint{","})?)?
     *   ;
     */
    private void parseTypeParameterList() {
        PsiBuilder.Marker list = mark();
        if (tt() == LT) {
            advance(); // LT

            while (true) {
                parseTypeParameter();

                if (!at(COMMA)) break;
                advance(); // COMMA
            }

            expect(GT, "Missing '>'", TYPE_PARAMETER_GT_RECOVERY_SET);
        }
        // TODO : where an stuff
        list.done(TYPE_PARAMETER_LIST);
    }

    /*
     * typeParameter
     *   : modifiers SimpleName (":" userType)?
     *   ;
     */
    private void parseTypeParameter() {
        if (TYPE_PARAMETER_GT_RECOVERY_SET.contains(tt())) {
            error("Type parameter declaration expected");
            return;
        }

        PsiBuilder.Marker mark = mark();
        parseModifierList();
        expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

        // TODO : other
        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }

        mark.done(TYPE_PARAMETER);

    }

    /*
     * type
     *   : attributes (userType | functionType | tupleType)
     *   ;
     */
    private void parseTypeRef() {
        PsiBuilder.Marker type = mark();

        parseAttributeList();

        while (true) {
            if (at(IDENTIFIER)) {
                parseSimpleUserType();
            }
            else if (at(LBRACE)) {
                parseSimpleFunctionType();
            } else if (at(LPAR)) {
                parseTupleType();
            } else {
                error("Type expected");
                break;
            }

            if (!at(DOT)) break;
            advance(); // DOT
        }
        type.done(TYPE_REFERENCE);
    }

    /*
     * userType
     *   : simpleUserType{"."}
     *   ;
     */
    private void parseUserType() {
        PsiBuilder.Marker userType = mark();

        while (true) {
            parseSimpleUserType();
            if (!at(DOT)) break;
            advance(); // DOT
        }

        userType.done(USER_TYPE);
    }

    /*
     * simpleUserType
     *   : SimpleName ("<" (optionalProjection type){","} ">")?
     *   ;
     */
    private void parseSimpleUserType() {
        PsiBuilder.Marker type = mark();

        expect(IDENTIFIER, "Type name expected", TokenSet.create(LT));
        parseTypeArgumentList();

        type.done(USER_TYPE);
    }

    /*
     *   (optionalProjection type){","}
     */
    private void parseTypeArgumentList() {
        if (!at(LT)) return;

        PsiBuilder.Marker list = mark();

        advance(); // LT

        while (true) {
            parseModifierList();
            parseTypeRef();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        expect(GT, "Expecting a '>'");

        list.done(TYPE_ARGUMENT_LIST);
    }

    /*
     * tupleType
     *   : "(" type{","}? ")"
     *   : "(" parameter{","} ")" // tuple with named entries, the names do not affect assignment compatibility
     *   ;
     */
    private void parseTupleType() {
        assert at(LPAR);

        PsiBuilder.Marker tuple = mark();

        advance(); // LPAR

        if (!at(RPAR)) {
            while (true) {
                if (at(COLON)) errorAndAdvance("Expecting a name for tuple entry");

                if (at(IDENTIFIER) && lookahead(1) == COLON) {
                    PsiBuilder.Marker labeledEntry = mark();
                    advance(); // IDENTIFIER
                    advance(); // COLON
                    parseTypeRef();
                    labeledEntry.done(LABELED_TUPLE_ENTRY);
                }
                else if (TYPE_REF_FIRST.contains(tt())) {
                    parseTypeRef();
                }
                else {
                    error("Type expected");
                    break;
                }
                if (!at(COMMA)) break;
                advance(); // COMMA
            }
        }

        expect(RPAR, "Expecting ')");

        tuple.done(TUPLE_TYPE);
    }

    /*
     * simpleFunctionType
     *   : "{" functionTypeContents "}"
     *   ;
     */
    private void parseSimpleFunctionType() {
        assert at(LBRACE);
        PsiBuilder.Marker functionType = mark();

        advance(); // LBRACE

        parseFunctionTypeContents();

        expect(RBRACE, "Expecting '}");
        functionType.done(FUNCTION_TYPE);
    }

    /*
     * functionTypeContents
     *   : "(" (parameter | type){","} ")" ":" type
     *   ;
     */
    private void parseFunctionTypeContents() {
        PsiBuilder.Marker parameters = mark();
        expect(LPAR, "Expecting '(");

        if (!at(RPAR)) {
            while (true) {
                if (!parseValueParameter()) {
                    parseModifierList(); // lazy, out, ref
                    parseTypeRef();
                }
                if (!at(COMMA)) break;
                advance(); // COMMA
            }
        }
        expect(RPAR, "Expecting ')'");
        parameters.done(VALUE_PARAMETER_LIST);

        expect(COLON, "Expecting ':' followed by a return type", TYPE_REF_FIRST);

        parseTypeRef();
    }

    /*
     * functionParameter
     *   : modifiers functionParameterRest
     *   ;
     */
    private boolean parseValueParameter() {
        PsiBuilder.Marker parameter = mark();

        parseModifierList();
        if (!parseFunctionParameterRest()) {
            parameter.rollbackTo();
            return false;
        }

        parameter.done(VALUE_PARAMETER);
        return true;
    }

}