/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.formatter;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.LightIdeaTestCase;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

// Based on from com.intellij.psi.formatter.java.AbstractJavaFormatterTest
@SuppressWarnings("UnusedDeclaration")
public abstract class AbstractJetFormatterTest extends LightIdeaTestCase {

    protected enum Action {REFORMAT, INDENT}

    private interface TestFormatAction {
        void run(PsiFile psiFile, int startOffset, int endOffset);
    }

    private static final Map<Action, TestFormatAction> ACTIONS = new EnumMap<Action, TestFormatAction>(Action.class);
    static {
        ACTIONS.put(Action.REFORMAT, new TestFormatAction() {
            @Override
            public void run(PsiFile psiFile, int startOffset, int endOffset) {
                CodeStyleManager.getInstance(getProject()).reformatText(psiFile, startOffset, endOffset);
            }
        });
        ACTIONS.put(Action.INDENT, new TestFormatAction() {
            @Override
            public void run(PsiFile psiFile, int startOffset, int endOffset) {
                CodeStyleManager.getInstance(getProject()).adjustLineIndent(psiFile, startOffset);
            }
        });
    }

    private static final String BASE_PATH =
            new File(PluginTestCaseBase.getTestDataPathBase(), "/formatter/").getAbsolutePath();

    public TextRange myTextRange;
    public TextRange myLineRange;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LanguageLevelProjectExtension.getInstance(getProject()).setLanguageLevel(LanguageLevel.HIGHEST);
    }

    public void doTest() throws Exception {
        doTest(getTestName(false) + ".kt", getTestName(false) + "_after.kt");
    }

    public void doTest(@NonNls String fileNameBefore, @NonNls String fileNameAfter) throws Exception {
        doTextTest(Action.REFORMAT, loadFile(fileNameBefore), loadFile(fileNameAfter), "");
    }

    public void doTextTest(@NonNls String text, @NonNls String textAfter, String commentToTextCompare) throws IncorrectOperationException {
        doTextTest(Action.REFORMAT, text, textAfter, commentToTextCompare);
    }

    public void doIndentTextTest(@NonNls String text, @NonNls String textAfter) throws IncorrectOperationException {
        doTextTest(Action.INDENT, text, textAfter, "");
    }

    public void doTextTest(final Action action, final String text, String textAfter, String commentToTextCompare) throws IncorrectOperationException {
        final PsiFile file = createFile("A.kt", text);

        if (myLineRange != null) {
            DocumentImpl document = new DocumentImpl(text);
            myTextRange =
                    new TextRange(document.getLineStartOffset(myLineRange.getStartOffset()), document.getLineEndOffset(myLineRange.getEndOffset()));
        }

        final PsiDocumentManager manager = PsiDocumentManager.getInstance(getProject());
        final Document document = manager.getDocument(file);

        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
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
                            assertTrue(e.getLocalizedMessage(), false);
                        }
                    }
                });
            }
        }, "", "");


        if (document == null) {
            fail("Don't expect the document to be null");
            return;
        }
        assertEquals(commentToTextCompare, prepareText(textAfter), prepareText(document.getText()));
        manager.commitDocument(document);
        assertEquals(commentToTextCompare, prepareText(textAfter), prepareText(file.getText()));
    }

    private static String prepareText(String actual) {
        if (actual.startsWith("\n")) {
            actual = actual.substring(1);
        }
        if (actual.startsWith("\n")) {
            actual = actual.substring(1);
        }

        // Strip trailing spaces
        final Document doc = EditorFactory.getInstance().createDocument(actual);
        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        ((DocumentImpl)doc).stripTrailingSpaces();
                    }
                });
            }
        }, "formatting", null);

        return doc.getText();
    }

    protected static String loadFile(String name) throws Exception {
        String text = FileUtil.loadFile(new File(BASE_PATH, name));
        text = StringUtil.convertLineSeparators(text);
        return text;
    }
}
