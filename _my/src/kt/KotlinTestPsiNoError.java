package kt;

/**
 * Created by user on 8/8/14.
 */
public class KotlinTestPsiNoError extends AbstractParsingTestCase {
    public KotlinTestPsiNoError() {
        super("psi", "kt", new KotlinParserDefinition());
    }

    public void testAbsentInnerType() { doTest(true); }
    public void testAnnotatedExpressions() { doTest(true); }
    public void testAnonymousInitializer() { doTest(true); }
    public void testAssertNotNull() { doTest(true); }
    public void testAttributes_ERR() { doTest(true); }
    public void testAttributes() { doTest(true); }
    public void testAttributesOnPatterns() { doTest(true); }
    public void testBabySteps_ERR() { doTest(true); }
    public void testBabySteps() { doTest(true); }
    public void testBackslashInString() { doTest(true); }
    public void testBlockCommentAtBeginningOfFile1() { doTest(true); }
    public void testBlockCommentAtBeginningOfFile2() { doTest(true); }
    public void testBlockCommentAtBeginningOfFile3() { doTest(true); }
    public void testBlockCommentAtBeginningOfFile4() { doTest(true); }
    public void testByClauses() { doTest(true); }
    public void testCallsInWhen() { doTest(true); }
    public void testCallWithManyClosures() { doTest(true); }
    public void testConstructors() { doTest(true); }
    public void testControlStructures() { doTest(true); }
    public void testDoubleColon() { doTest(true); }
    public void testEmptyFile() { doTest(true); }
    public void testEnums() { doTest(true); }
    public void testEOLsInComments() { doTest(true); }
    public void testEOLsOnRollback() { doTest(true); }
    public void testExpressions_ERR() { doTest(true); }
    public void testExtensionsWithQNReceiver() { doTest(true); }
    public void testFileStart_ERR() { doTest(true); }
    public void testFloatingPointLiteral() { doTest(true); }
    public void testForWithMultiDecl() { doTest(true); }
    public void testFunctionCalls() { doTest(true); }
    public void testFunctionLiterals_ERR() { doTest(true); }
    public void testFunctionLiterals() { doTest(true); }
    public void testFunctionNoParameterList() { doTest(true); }
    public void testFunctions_ERR() { doTest(true); }
    public void testFunctions() { doTest(true); }
    public void testFunctionTypes_ERR() { doTest(true); }
    public void testFunctionTypes() { doTest(true); }
    public void testHangOnLonelyModifier() { doTest(true); }
    public void testIfWithPropery() { doTest(true); }
    public void testImports_ERR() { doTest(true); }
    public void testImports() { doTest(true); }
    public void testImportSoftKW() { doTest(true); }
    public void testInner() { doTest(true); }
    public void testIntegerLiteral() { doTest(true); }
    public void testLabels() { doTest(true); }
    public void testLocalDeclarations() { doTest(true); }
    public void testLongPackageName() { doTest(true); }
    public void testModifierAsSelector() { doTest(true); }
    public void testMultiVariableDeclarations() { doTest(true); }
    public void testNestedComments() { doTest(true); }
    public void testNewlinesInParentheses() { doTest(true); }
    public void testNewLinesValidOperations() { doTest(true); }
    public void testNotIsAndNotIn() { doTest(true); }
    public void testObjectLiteralAsStatement() { doTest(true); }
    public void testPackageBlockFirst() { doTest(true); }
    public void testPackageModifiers() { doTest(true); }
    public void testParameterNameMising() { doTest(true); }
    public void testPrecedence() { doTest(true); }
    public void testPrimaryConstructorModifiers_ERR() { doTest(true); }
    public void testProperties_ERR() { doTest(true); }
    public void testPropertiesFollowedByInitializers() { doTest(true); }
    public void testProperties() { doTest(true); }
    public void testQuotedIdentifiers() { doTest(true); }
    public void testRootPackage() { doTest(true); }
    public void testSemicolonAfterIf() { doTest(true); }
    public void testShortAnnotations() { doTest(true); }
    public void testSimpleClassMembers_ERR() { doTest(true); }
    public void testSimpleClassMembers() { doTest(true); }
    public void testSimpleExpressions() { doTest(true); }
    public void testSimpleModifiers() { doTest(true); }
    public void testSoftKeywordsInTypeArguments() { doTest(true); }
    public void testSoftKeywords() { doTest(true); }
    public void testStringTemplates() { doTest(true); }
    public void testSuper() { doTest(true); }
    public void testThisType() { doTest(true); }
    public void testTryRecovery() { doTest(true); }
    public void testTypeAnnotations() { doTest(true); }
    public void testTypeConstraints() { doTest(true); }
    public void testTypeDef_ERR() { doTest(true); }
    public void testTypeDef() { doTest(true); }
    public void testTypeExpressionAmbiguities_ERR() { doTest(true); }
    public void testTypeParametersBeforeName() { doTest(true); }
    public void testWhen_ERR() { doTest(true); }
    public void testWhen() { doTest(true); }



}
