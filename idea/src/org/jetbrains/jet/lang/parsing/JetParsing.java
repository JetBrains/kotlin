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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author max
 * @author abreslav
 */
public class JetParsing extends AbstractJetParsing {
    // TODO: token sets to constants

    private static final Map<String, IElementType> MODIFIER_KEYWORD_MAP = new HashMap<String, IElementType>();
    static {
        for (IElementType softKeyword : MODIFIER_KEYWORDS.getTypes()) {
            MODIFIER_KEYWORD_MAP.put(((JetKeywordToken) softKeyword).getValue(), softKeyword);
        }
    }

    private static final TokenSet TOPLEVEL_OBJECT_FIRST = TokenSet.create(TYPE_KEYWORD, CLASS_KEYWORD,
                EXTENSION_KEYWORD, FUN_KEYWORD, VAL_KEYWORD, NAMESPACE_KEYWORD, DECOMPOSER_KEYWORD);
    private static final TokenSet ENUM_MEMBER_FIRST = TokenSet.create(TYPE_KEYWORD, CLASS_KEYWORD,
                EXTENSION_KEYWORD, FUN_KEYWORD, VAL_KEYWORD, DECOMPOSER_KEYWORD, IDENTIFIER);

    private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, WRAPS_KEYWORD, LPAR, COLON, LBRACE), TOPLEVEL_OBJECT_FIRST);
    private static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, WRAPS_KEYWORD, LPAR, COLON, LBRACE, GT);
    private static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR);
    private static final TokenSet NAMESPACE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON);
    /*package*/ static final TokenSet TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LBRACE, LPAR);

    private final JetExpressionParsing myExpressionParsing;

    public JetParsing(SemanticWhitespaceAwarePsiBuilder builder) {
        super(builder);
        this.myExpressionParsing = new JetExpressionParsing(builder, this);
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
         *   : "namespace" SimpleName{"."} SEMI?
         *   ;
         */
        if (at(NAMESPACE_KEYWORD)) {
            advance(); // NAMESPACE_KEYWORD

            parseNamespaceName();

            consumeIf(SEMICOLON);
        }

        while (at(IMPORT_KEYWORD)) {
            parseImportDirective();
        }
    }

    /* SimpleName{"."} */
    private void parseNamespaceName() {
        PsiBuilder.Marker nsName = mark();
        expect(IDENTIFIER, "Expecting qualified name", NAMESPACE_NAME_RECOVERY_SET);
        while (!eol() && at(DOT)) {
            advance(); // DOT
            expect(IDENTIFIER, "Namespace name must be a '.'-separated identifier list", NAMESPACE_NAME_RECOVERY_SET);
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

        EnumDetector detector = new EnumDetector();
        parseModifierList(detector);

        IElementType keywordToken = tt();
        JetNodeType declType = null;
        if (keywordToken == NAMESPACE_KEYWORD) {
            declType = parseNamespaceBlock();
        }
        else if (keywordToken == CLASS_KEYWORD) {
            declType = parseClass(detector.isEnum());
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
            declType = parseTypeDef();
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
     *   : modifiers "decomposer" (type ".")? SimpleName? "(" (attributes SimpleName){","}? ")" // Public properties only
     *   ;
     */
    private JetNodeType parseDecomposer() {
        assert at(DECOMPOSER_KEYWORD);
        advance(); // DECOMPOSER_KEYWORD

        boolean extenstion;
        if (!at(LPAR)) {
            extenstion = true;
            if (TYPE_REF_FIRST.contains(tt())
                    && !(at(IDENTIFIER) && lookahead(1) == LPAR)) {
                // TODO: if this type is annotated with an attribute, and it is a single identifier, it is a error (decomposer [a] foo())
                parseTypeRef();
                // The decomposer name may appear as the last section of the type
                if (at(DOT)) {
                    advance(); // DOT
                    expect(IDENTIFIER, "Expecting decomposer name", TokenSet.create(LPAR));
                }
            }
            else {
                consumeIf(IDENTIFIER);
            }
        } else {
            extenstion = false;
        }

        PsiBuilder.Marker properties = mark();

        expect(LPAR, "Expecting a property list in parentheses '( ... )'");

        // Property list
        if (!at(RPAR)) {
            while (true) {
                parseAttributeList();
                if (!expect(IDENTIFIER, "Expecting a property name", TokenSet.create(COMMA, RPAR))) {
                    skipUntil(TokenSet.create(COMMA, RPAR, EOL_OR_SEMICOLON));
                }
                if (!at(COMMA)) {
                    if (at(RPAR)) break;
                    error("Expecting ',' or a closing ')'");
                    skipUntil(TokenSet.create(COMMA, RPAR, EOL_OR_SEMICOLON));
                }
                if (!at(COMMA)) break;
                advance(); // COMMA
            }
        }

        expect(RPAR, "Expecting ')' to close a property list");

        consumeIf(SEMICOLON);

        properties.done(DECOMPOSER_PROPERTY_LIST);

        if (at(DOT) && !extenstion) {
            error("Cannot define an extension decomposer on a tuple");
        }

        return DECOMPOSER;
    }

    /*
     * (modifier | attribute)*
     */
    private void parseModifierList() {
        parseModifierList(null);
    }

    /**
     * (modifier | attribute)*
     *
     * Feeds modifiers (not attributes) into the passed consumer, if it is not null
     */
    private void parseModifierList(Consumer<IElementType> tokenConsumer) {
        PsiBuilder.Marker list = mark();
        boolean empty = true;
        while (!eof()) {
            if (at(LBRACKET)) {
                parseAttributeAnnotation();
            } else if (atSet(MODIFIER_KEYWORDS)) {
                if (tokenConsumer != null) tokenConsumer.consume(tt());
                advance(); // MODIFIER
            }
            else {
                break;
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
        if (at(LPAR)) myExpressionParsing.parseValueArgumentList();
        attribute.done(ATTRIBUTE);
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
        assert at(NAMESPACE_KEYWORD);
        advance(); // NAMESPACE_KEYWORD

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
     *       (classBody? | enumClassBody)
     *   ;
     */
    private JetNodeType parseClass(boolean enumClass) {
        assert at(CLASS_KEYWORD);
        advance(); // CLASS_KEYWORD

        expect(IDENTIFIER, "Class name expected", CLASS_NAME_RECOVERY_SET);
        parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);

        consumeIf(WRAPS_KEYWORD);
        if (at(LPAR)) {
            parsePrimaryConstructorParameterList();
        }

        if (at(COLON)) {
            advance(); // COLON
            parseDelegationSpecifierList();
        }

        if (at(LBRACE)) {
            if (enumClass) {
                parseEnumClassBody();
            }
            else {
                parseClassBody();
            }
        }

        return CLASS;
    }

    /*
     * enumClassBody
     *   : "{" enumEntry* "}"
     *   ;
     */
    private void parseEnumClassBody() {
        if (!at(LBRACE)) return;

        PsiBuilder.Marker classBody = mark();

        advance(); // LBRACE

        while (!eof() && !at(RBRACE)) {
            PsiBuilder.Marker entryOrMember = mark();

            TokenSet constructorNameFollow = TokenSet.create(SEMICOLON, COLON, LPAR, LT, LBRACE);
            int lastId = findLastBefore(ENUM_MEMBER_FIRST, constructorNameFollow, false);
            EnumDetector enumDetector = new EnumDetector();
            createTruncatedBuilder(lastId).parseModifierList(enumDetector);

            IElementType type;
            if (at(IDENTIFIER)) {
                parseEnumEntry();
                type = ENUM_ENTRY;
            }
            else {
                type = parseMemberDeclarationRest(enumDetector.isEnum());
            }

            if (type == null) {
                errorAndAdvance("Expecting an enum entry or member declaration");
                entryOrMember.drop();
            }
            else {
                entryOrMember.done(type);
            }
        }

        expect(RBRACE, "Expecting '}' to close enum class body");

        classBody.done(CLASS_BODY);
    }

    /*
     * enumEntry
     *   : modifiers SimpleName typeParameters? valueParameters? (":" initializer{","})? classBody?
     *   ;
     */
    private void parseEnumEntry() {
        assert at(IDENTIFIER);

        advance(); // IDENTIFIER

        parseTypeParameterList(TokenSet.create(COLON, LPAR, SEMICOLON, LBRACE));

        if (at(LPAR)) {
            parseValueParameterList(false, TokenSet.create(COLON, SEMICOLON, LBRACE));
        }

        if (at(COLON)) {
            advance(); // COLON

            parseInitializerList();
        }

        if (at(LBRACE)) {
            parseClassBody();
        }

        consumeIf(SEMICOLON);
    }

    /*
     * classBody
     *   : ("{" memberDeclaration{","} "}")?
     *   ;
     */
    /*package*/ void parseClassBody() {
        assert at(LBRACE);
        PsiBuilder.Marker body = mark();
        advance(); // LBRACE

        while (!eof()) {
            if (at(RBRACE)) {
                break;
            }
            parseMemberDeclaration();
        }
        expect(RBRACE, "Missing '}");
        body.done(CLASS_BODY);
    }

    /*
     * memberDeclaration
     *   : classObject
     *   : constructor
     *   : decomposer
     *   : function
     *   : property
     *   : class
     *   : extension
     *   : typedef
     *   ;
     */
    private void parseMemberDeclaration() {
        PsiBuilder.Marker decl = mark();

        EnumDetector enumDetector = new EnumDetector();
        parseModifierList(enumDetector);

        JetNodeType declType = parseMemberDeclarationRest(enumDetector.isEnum());

        if (declType == null) {
            errorAndAdvance("Expecting member declaration");
            decl.drop();
        }
        else {
            decl.done(declType);
        }
    }

    private JetNodeType parseMemberDeclarationRest(boolean isEnum) {
        IElementType keywordToken = tt();
        JetNodeType declType = null;
        if (keywordToken == CLASS_KEYWORD) {
            if (lookahead(1) == OBJECT_KEYWORD) {
                declType = parseClassObject();
            }
            else {
                declType = parseClass(isEnum);
            }
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
            declType = parseTypeDef();
        }
        else if (keywordToken == DECOMPOSER_KEYWORD) {
            declType = parseDecomposer();
        }
        else if (keywordToken == THIS_KEYWORD) {
            declType = parseConstructor();
        }
        return declType;
    }

    /*
     * constructor
     *   : modifiers "this" functionParameters (":" initializer{","}) block?
     *   ;
     */
    private JetNodeType parseConstructor() {
        assert at(THIS_KEYWORD);

        advance(); // THIS_KEYWORD

        parseValueParameterList(false, TokenSet.create(COLON, LBRACE, SEMICOLON));

        if (at(COLON)) {
            advance(); // COLON

            parseInitializerList();
        }

        if (at(LBRACE)) {
            parseBlock();
        }
        else {
            consumeIf(SEMICOLON);
        }

        return CONSTRUCTOR;
    }

    /*
     * initializer{","}
     */
    private void parseInitializerList() {
        PsiBuilder.Marker list = mark();
        while (true) {
            if (at(COMMA)) errorAndAdvance("Expecting a this or super constructor call");
            parseInitializer();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }
        list.done(INITIALIZER_LIST);
    }

    /*
     * initializer
     *   : attributes "this" valueArguments
     *   : attributes constructorInvocation // type parameters may (must?) be omitted
     *   ;
     */
    private void parseInitializer() {
        PsiBuilder.Marker initializer = mark();
        parseAttributeList();

        IElementType type;
        if (at(THIS_KEYWORD)) {
            advance(); // THIS_KEYWORD
            type = THIS_CALL;
        }
        else if (atSet(TYPE_REF_FIRST)) {
            parseTypeRef();
            type = DELEGATOR_SUPER_CALL;
        } else {
            errorWithRecovery("Expecting constructor call (this(...)) or supertype initializer", TokenSet.create(LBRACE, COMMA));
            initializer.drop();
            return;
        }
        myExpressionParsing.parseValueArgumentList();

        initializer.done(type);
    }

    /*
     * classObject
     *   : modifiers "class" objectLiteral
     *   ;
     */
    private JetNodeType parseClassObject() {
        assert at(CLASS_KEYWORD) && lookahead(1) == OBJECT_KEYWORD;

        advance(); // CLASS_KEYWORD

        myExpressionParsing.parseObjectLiteral();


        return CLASS_OBJECT;
    }

    /*
     * typedef
     *   : modifiers "type" SimpleName typeParameters? "=" type
     *   ;
     */
    private JetNodeType parseTypeDef() {
        assert at(TYPE_KEYWORD);

        advance(); // TYPE_KEYWORD

        expect(IDENTIFIER, "Type name expected", TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOPLEVEL_OBJECT_FIRST));

        parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);

        expect(EQ, "Expecting '='", TokenSet.orSet(TOPLEVEL_OBJECT_FIRST, TokenSet.create(SEMICOLON)));

        parseTypeRef();

        consumeIf(SEMICOLON);

        return TYPEDEF;
    }

    /*
     * property
     *   : modifiers ("val" | "var") attributes (type ".")? SimpleName (":" type)? ("=" expression)?
     *       (getter? setter? | setter? getter?) SEMI?
     *   ;
     */
    private JetNodeType parseProperty() {
        assert at(VAL_KEYWORD) || at(VAR_KEYWORD);

        advance(); // VAL_KEYWORD or VAR_KEYWORD

        // TODO: EOL_OR_SEMICOLON is too restrictive here
        TokenSet propertyNameFollow = TokenSet.create(COLON, EQ, LBRACE, SEMICOLON, EOL_OR_SEMICOLON);

        int lastDot = findLastBefore(TokenSet.create(DOT), propertyNameFollow, true);

        if (lastDot == -1) {
            parseAttributeList();
            expect(IDENTIFIER, "Expecting property name or receiver type", propertyNameFollow);
        }
        else {
            createTruncatedBuilder(lastDot).parseTypeRef();

            expect(DOT, "Expecting '.' before a property name", propertyNameFollow);
            expect(IDENTIFIER, "Expecting property name", propertyNameFollow);
        }

        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }

        if (at(EQ)) {
            advance(); // EQ
            myExpressionParsing.parseExpression();
        }

        if (parsePropertyGetterOrSetter()) {
            parsePropertyGetterOrSetter();
        }

        consumeIf(SEMICOLON);

        return PROPERTY;
    }

    /*
     * getter
     *   : modifiers
     *        (     "get" "(" ")"
     *           |
     *              "set" "(" modifiers parameter ")"
     *        ) functionBody
     *   ;
     */
    private boolean parsePropertyGetterOrSetter() {
        PsiBuilder.Marker getterOrSetter = mark();

        parseModifierList();

        if (!at(GET_KEYWORD) && !at(SET_KEYWORD)) {
            getterOrSetter.rollbackTo();
            return false;
        }

        boolean setter = at(SET_KEYWORD);
        advance(); // GET_KEYWORD or SET_KEYWORD

        expect(LPAR, "Expecting '('", TokenSet.create(RPAR, IDENTIFIER, COLON, LBRACE, EQ));
        if (setter) {
            PsiBuilder.Marker setterParameter = mark();
            int lastId = findLastBefore(TokenSet.create(IDENTIFIER), TokenSet.create(RPAR, COMMA, COLON), false);
            createTruncatedBuilder(lastId).parseModifierList();
            expect(IDENTIFIER, "Expecting parameter name", TokenSet.create(RPAR, COLON, LBRACE, EQ));

            if (at(COLON)) {
                advance();

                parseTypeRef();
            }
            setterParameter.done(VALUE_PARAMETER);
        }
        if (!at(RPAR)) errorUntil("Expecting ')'", TokenSet.create(RPAR, COLON, LBRACE, EQ, EOL_OR_SEMICOLON));
        expect(RPAR, "Expecting ')'", TokenSet.create(RPAR, COLON, LBRACE, EQ));

        if (at(COLON)) {
            advance();

            parseTypeRef();
        }

        parseFunctionBody();

        getterOrSetter.done(PROPERTY_ACCESSOR);

        return true;
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
        assert at(FUN_KEYWORD);

        advance(); // FUN_KEYWORD

        int lastDot = findLastBefore(TokenSet.create(DOT), TokenSet.create(LPAR), true);

        if (lastDot == -1) { // There's no explicit receiver type specified
            parseAttributeList();
            expect(IDENTIFIER, "Expecting function name or receiver type");
        } else {
            createTruncatedBuilder(lastDot).parseTypeRef();

            TokenSet functionNameFollow = TokenSet.create(LT, LPAR, COLON, EQ);
            expect(DOT, "Expecting '.' before a function name", functionNameFollow);
            expect(IDENTIFIER, "Expecting function name", functionNameFollow);
        }

        TokenSet valueParametersFollow = TokenSet.create(COLON, EQ, LBRACE, SEMICOLON, RPAR);

        parseTypeParameterList(TokenSet.orSet(TokenSet.create(LPAR), valueParametersFollow));


        parseValueParameterList(false, valueParametersFollow);

        if (at(COLON)) {
            advance(); // COLON

            parseTypeRef();
        }

        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
        }
        else if (at(EQ) || at(LBRACE)) {
            parseFunctionBody();
        }

        return FUN;
    }

    /*
     * functionBody
     *   : block
     *   : "=" expression
     *   ;
     */
    private void parseFunctionBody() {
        if (at(LBRACE)) {
            parseBlock();
        }
        else if (at(EQ)) {
            advance(); // EQ
            myExpressionParsing.parseExpression();
            consumeIf(SEMICOLON);
        }
        else {
            errorAndAdvance("Expecting function body");
        }
    }

    /*
     * block
     *   : "{" (expression SEMI)* "}"
     *   ;
     */
    public void parseBlock() {
        PsiBuilder.Marker block = mark();

        expect(LBRACE, "Expecting '{' to open a block");

        while (!eof() && !at(RBRACE)) {
            myExpressionParsing.parseExpression();
            consumeIf(SEMICOLON);
        }

        expect(RBRACE, "Expecting '}");

        block.done(BLOCK);
    }

    /*
     * extension
     *   : modifiers "extension" SimpleName? typeParameters? "for" type classBody? // properties cannot be lazy, cannot have backing fields
     *   ;
     */
    private JetNodeType parseExtension() {
        assert at(EXTENSION_KEYWORD);

        advance(); // EXTENSION_KEYWORD

        consumeIf(IDENTIFIER);

        parseTypeParameterList(TokenSet.create(FOR_KEYWORD, LBRACE));

        expect(FOR_KEYWORD, "Expecting 'for' to specify the type that is being extended", TYPE_REF_FIRST);

        parseTypeRef();

        if (at(LBRACE)) {
            parseClassBody();
        }
        else {
            consumeIf(SEMICOLON);
        }

        return EXTENSION;
    }

    /*
     * delegationSpecifier{","}
     */
    /*package*/ void parseDelegationSpecifierList() {
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
        parseTypeRef();

        if (at(BY_KEYWORD)) {
            advance(); // BY_KEYWORD
            myExpressionParsing.parseExpression();
            delegator.done(DELEGATOR_BY);
        }
        else if (at(LPAR)) {
            myExpressionParsing.parseValueArgumentList();
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

        int lastId = findLastBefore(TokenSet.create(IDENTIFIER), TokenSet.create(COMMA, RPAR, COLON), false);
        createTruncatedBuilder(lastId).parseModifierList();

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
    private void parseTypeParameterList(TokenSet recoverySet) {
        PsiBuilder.Marker list = mark();
        if (at(LT)) {
            advance(); // LT

            while (true) {
                if (at(COMMA)) errorAndAdvance("Expecting type parameter declaration");
                parseTypeParameter();

                if (!at(COMMA)) break;
                advance(); // COMMA
            }

            expect(GT, "Missing '>'", recoverySet);

            if (at(WHERE_KEYWORD)) {
                parseTypeConstraintList();
            }
        }
        list.done(TYPE_PARAMETER_LIST);
    }

    /*
     * typeConstraint{","}
     */
    private void parseTypeConstraintList() {
        assert at(WHERE_KEYWORD);

        advance(); // WHERE_KEYWORD

        PsiBuilder.Marker list = mark();

        while (true) {
            if (at(COMMA)) errorAndAdvance("Type constraint expected");
            parseTypeConstraint();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        list.done(TYPE_CONSTRAINT_LIST);
    }

    /*
     * typeConstraint
     *   : userType ":" type
     *   : "class" "object" userType ":" type
     *   ;
     */
    private void parseTypeConstraint() {
        PsiBuilder.Marker constraint = mark();

        if (at(CLASS_KEYWORD)) {
            advance(); // CLASS_KEYWORD

            expect(OBJECT_KEYWORD, "Expecting 'object'", TYPE_REF_FIRST);

        }
        parseTypeRef();

        expect(COLON, "Expecting ':' before the upper bound", TYPE_REF_FIRST);

        parseTypeRef();

        constraint.done(TYPE_CONSTRAINT);
    }

    /*
     * typeParameter
     *   : modifiers SimpleName (":" userType)?
     *   ;
     */
    private void parseTypeParameter() {
        if (atSet(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            error("Type parameter declaration expected");
            return;
        }

        PsiBuilder.Marker mark = mark();

        int lastId = findLastBefore(TokenSet.create(IDENTIFIER), TokenSet.create(COMMA, GT, COLON), false);
        createTruncatedBuilder(lastId).parseModifierList();

        expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

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
    public void parseTypeRef() {
        PsiBuilder.Marker type = mark();

        parseAttributeList();

        TokenSet simpleTypeFirst = TokenSet.create(IDENTIFIER, LBRACE, LPAR);
        while (true) {
            if (at(IDENTIFIER)) {
                parseSimpleUserType();
            }
            else if (at(LBRACE)) {
                parseSimpleFunctionType();
            } else if (at(LPAR)) {
                parseTupleType();
            } else {
                errorWithRecovery("Type expected",
                        TokenSet.orSet(TOPLEVEL_OBJECT_FIRST,
                                TokenSet.create(EQ, COMMA, GT, RBRACKET, DOT, RPAR, RBRACE, LBRACE, SEMICOLON)));
                break;
            }

            if (!at(DOT)) break;
            if (!simpleTypeFirst.contains(lookahead(1))) break;
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

        userType.done(TYPE_REFERENCE);
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
                if (at(COLON)) {
                    errorAndAdvance("Expecting a name for tuple entry");
                }

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
        parseValueParameterList(true, TokenSet.EMPTY);

        expect(COLON, "Expecting ':' followed by a return type", TYPE_REF_FIRST);

        parseTypeRef();
    }

    /*
     * functionParameters
     *   : "(" functionParameter{","}? ")" // default values
     *   ;
     *
     * functionParameter
     *   : modifiers functionParameterRest
     *   ;
     *
     * functionParameterRest
     *   : parameter ("=" expression)?
     *   ;
     */
    public void parseValueParameterList(boolean isFunctionTypeContents, TokenSet recoverySet) {
        PsiBuilder.Marker parameters = mark();
        expect(LPAR, "Expecting '(", recoverySet);

        if (!at(RPAR)) {
            while (true) {
                if (at(COMMA)) {
                    errorAndAdvance("Expecting a parameter declaration");
                }
                else if (at(RPAR)) {
                    error("Expecting a parameter declaration");
                    break;
                }
                if (isFunctionTypeContents) {
                    if (!tryParseValueParameter()) {
                            parseModifierList(); // lazy, out, ref
                            parseTypeRef();
                    }
                } else {
                    parseValueParameter();
                }
                if (!at(COMMA)) break;
                advance(); // COMMA
            }
        }
        expect(RPAR, "Expecting ')'", recoverySet);
        parameters.done(VALUE_PARAMETER_LIST);
    }

    /*
     * functionParameter
     *   : modifiers functionParameterRest
     *   ;
     */
    private boolean tryParseValueParameter() {
        PsiBuilder.Marker parameter = mark();

        int lastId = findLastBefore(TokenSet.create(IDENTIFIER), TokenSet.create(COMMA, RPAR, COLON), false);
        createTruncatedBuilder(lastId).parseModifierList();

        if (!parseFunctionParameterRest()) {
            parameter.rollbackTo();
            return false;
        }

        parameter.done(VALUE_PARAMETER);
        return true;
    }

    /*
     * functionParameter
     *   : modifiers functionParameterRest
     *   ;
     */
    private void parseValueParameter() {
        PsiBuilder.Marker parameter = mark();

        int lastId = findLastBefore(TokenSet.create(IDENTIFIER), TokenSet.create(COMMA, RPAR, COLON), false);
        createTruncatedBuilder(lastId).parseModifierList();

        parseFunctionParameterRest();

        parameter.done(VALUE_PARAMETER);
    }

    private JetParsing createTruncatedBuilder(int eofPosition) {
        return new JetParsing(new TruncatedSemanticWhitespaceAwarePsiBuilder(myBuilder, eofPosition));
    }

    private static class EnumDetector implements Consumer<IElementType> {

        private boolean myEnum = false;

        @Override
        public void consume(IElementType item) {
            if (item == ENUM_KEYWORD) {
                myEnum = true;
            }
        }

        public boolean isEnum() {
            return myEnum;
        }
    }
}