/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formatter;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings;
import org.jetbrains.kotlin.idea.test.KotlinLightIdeaTestCase;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.SettingsConfigurator;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

// Based on from com.intellij.psi.formatter.java.AbstractJavaFormatterTest
@SuppressWarnings("UnusedDeclaration")
public abstract class AbstractFormatterTest extends KotlinLightIdeaTestCase {

    protected enum Action {REFORMAT, INDENT}

    private interface TestFormatAction {
        void run(PsiFile psiFile, int startOffset, int endOffset);
    }

    private static final Map<Action, TestFormatAction> ACTIONS = new EnumMap<Action, TestFormatAction>(Action.class);

    static {
        ACTIONS.put(Action.REFORMAT,
                    (psiFile, startOffset, endOffset) -> CodeStyleManager.getInstance(psiFile.getProject())
                            .reformatText(psiFile, startOffset, endOffset));
        ACTIONS.put(Action.INDENT,
                    (psiFile, startOffset, endOffset) -> CodeStyleManager.getInstance(psiFile.getProject())
                            .adjustLineIndent(psiFile, startOffset));
    }

    private static final String BASE_PATH =
            new File(PluginTestCaseBase.getTestDataPathBase(), "/formatter/").getAbsolutePath();

    public TextRange myTextRange;
    public TextRange myLineRange;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LanguageLevelProjectExtension.getInstance(getProject()).setLanguageLevel(LanguageLevel.HIGHEST);
        Registry.get("kotlin.formatter.allowTrailingCommaInAnyProject").setValue(true);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Registry.get("kotlin.formatter.allowTrailingCommaInAnyProject").resetToDefault();
    }

    public void doTextTest(@NonNls String text, File fileAfter, String extension) throws IncorrectOperationException {
        doTextTest(Action.REFORMAT, text, fileAfter, extension);
    }

    public void doTextTest(Action action, String text, File fileAfter, String extension) throws IncorrectOperationException {
        PsiFile file = createFile("A" + extension, text);

        if (myLineRange != null) {
            DocumentImpl document = new DocumentImpl(text);
            myTextRange =
                    new TextRange(document.getLineStartOffset(myLineRange.getStartOffset()),
                                  document.getLineEndOffset(myLineRange.getEndOffset()));
        }

        PsiDocumentManager manager = PsiDocumentManager.getInstance(getProject());
        Document document = manager.getDocument(file);
        CommandProcessor.getInstance().executeCommand(getProject(), () -> ApplicationManager.getApplication().runWriteAction(() -> {
            document.replaceString(0, document.getTextLength(), text);
            manager.commitDocument(document);
            try {
                TextRange rangeToUse = myTextRange;
                if (rangeToUse == null) {
                    rangeToUse = file.getTextRange();
                }
                ACTIONS.get(action).run(file, rangeToUse.getStartOffset(), rangeToUse.getEndOffset());
            }
            catch (IncorrectOperationException e) {
                fail(e.getLocalizedMessage());
            }
        }), "", "");


        if (document == null) {
            fail("Don't expect the document to be null");
            return;
        }
        KotlinTestUtils.assertEqualsToFile(fileAfter, document.getText());
        manager.commitDocument(document);
        KotlinTestUtils.assertEqualsToFile(fileAfter, file.getText());
    }

    public void doTest(@NotNull String expectedFileNameWithExtension) throws Exception {
        doTest(expectedFileNameWithExtension, false);
    }

    public void doTestInverted(@NotNull String expectedFileNameWithExtension) throws Exception {
        doTest(expectedFileNameWithExtension, true);
    }

    public void doTest(@NotNull String expectedFileNameWithExtension, boolean inverted) throws Exception {
        String testFileName = expectedFileNameWithExtension.substring(0, expectedFileNameWithExtension.indexOf("."));
        String testFileExtension = expectedFileNameWithExtension.substring(expectedFileNameWithExtension.lastIndexOf("."));
        String originalFileText = FileUtil.loadFile(new File(testFileName + testFileExtension), true);

        CodeStyleSettings codeStyleSettings = CodeStyle.getSettings(getProject_());
        try {
            Integer rightMargin = InTextDirectivesUtils.getPrefixedInt(originalFileText, "// RIGHT_MARGIN: ");
            if (rightMargin != null) {
                codeStyleSettings.setRightMargin(KotlinLanguage.INSTANCE, rightMargin);
            }

            Boolean trailingComma = InTextDirectivesUtils.getPrefixedBoolean(originalFileText, "// TRAILING_COMMA: ");
            if (trailingComma != null) {
                codeStyleSettings.getCustomSettings(KotlinCodeStyleSettings.class).ALLOW_TRAILING_COMMA = trailingComma;
            }

            SettingsConfigurator configurator = FormatSettingsUtil.createConfigurator(originalFileText, codeStyleSettings);
            if (!inverted) {
                configurator.configureSettings();
            }
            else {
                configurator.configureInvertedSettings();
            }

            doTextTest(originalFileText, new File(expectedFileNameWithExtension), testFileExtension);
        }
        finally {
            codeStyleSettings.clearCodeStyleSettings();
        }
    }
}
