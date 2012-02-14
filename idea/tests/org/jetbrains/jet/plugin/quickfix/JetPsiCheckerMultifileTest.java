/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.util.ui.UIUtil;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * @author Nikolay Krasko
 */
public class JetPsiCheckerMultifileTest extends DaemonAnalyzerTestCase {

    public final static String MAIN_SUBSTRING = ".Main";
    public final static String DATA_SUBSTRING = ".Data";

    private final String dataPath;
    private final String name;

    public JetPsiCheckerMultifileTest(String dataPath, String name) {
        this.dataPath = dataPath;
        this.name = name;

        setName("testRun");
    }

    protected static boolean shouldBeAvailableAfterExecution() {
        return false;
    }

    public void testRun() throws Exception {
        configureByFiles(null, getFileNames(getTestFiles()).toArray(new String[1]));
        doTest();
    }

    public void doTest() {
        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
            @Override
            public void run() {
                try {
                    final Pair<String, Boolean> pair = LightQuickFixTestCase.parseActionHint(getFile(), loadFile(getFile().getName()));
                    final String text = pair.getFirst();
                    
                    final boolean actionShouldBeAvailable = pair.getSecond();
    
                    doAction(text, actionShouldBeAvailable, getTestDataPath());
                }
                catch (FileComparisonFailure e){
                    throw e;
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    fail(getTestName(true));
                }
            }
        }, "", "");
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public void doAction(final String text, final boolean actionShouldBeAvailable, final String testFullPath)
            throws Exception {
        IntentionAction action = LightQuickFixTestCase.findActionWithText(getAvailableActions(), text);
        if (action == null) {
            if (actionShouldBeAvailable) {
                List<IntentionAction> actions = getAvailableActions();
                List<String> texts = new ArrayList<String>();
                for (IntentionAction intentionAction : actions) {
                    texts.add(intentionAction.getText());
                }
                Collection<HighlightInfo> infos = doHighlighting();
                fail("Action with text '" + text + "' is not available in test " + testFullPath + "\n" +
                     "Available actions (" + texts.size() + "): " + texts + "\n" +
                     actions + "\n" +
                     "Infos:" + infos);
            }
        }
        else {
            if (!actionShouldBeAvailable) {
                fail("Action '" + text + "' is available (but must not) in test " + testFullPath);
            }

            ShowIntentionActionsHandler.chooseActionAndInvoke(getFile(), getEditor(), action, action.getText());

            UIUtil.dispatchAllInvocationEvents();

            if (!shouldBeAvailableAfterExecution()) {
                final IntentionAction afterAction = LightQuickFixTestCase.findActionWithText(getAvailableActions(), text);
                
                if (afterAction != null) {
                    fail("Action '" + text + "' is still available after its invocation in test " + testFullPath);
                }
            }

            checkResultByFile(name.replace("before", "after").replace(MAIN_SUBSTRING, "") + ".kt");
        }
    }
    
    protected List<File> getTestFiles() {
        File dir = new File(getTestDataPath());

        assertTrue("Main file should contain .Main. substring", name.contains(MAIN_SUBSTRING));
        final String testPrefix = name.replace(MAIN_SUBSTRING, "");
        
        // Files of single test
        FilenameFilter resultFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.contains(testPrefix) && !s.contains("after");
            }
        };

        List<File> allTestFiles = Arrays.asList(dir.listFiles(resultFilter));
        
        final Collection<File> mainFiles = Collections2.filter(allTestFiles, new Predicate<File>() {
            @Override
            public boolean apply(@Nullable File file) {
                return file != null && file.getName().contains(MAIN_SUBSTRING);
            }
        });

        assertTrue("No main file for test", mainFiles.size() > 0);
        assertTrue("Too many main files for the test", mainFiles.size() <= 1);

        final Collection<File> dataFiles = Collections2.filter(allTestFiles, new Predicate<File>() {
            @Override
            public boolean apply(@Nullable File file) {
                return file != null && file.getName().contains(DATA_SUBSTRING);
            }
        });

        final ArrayList<File> fileResult = new ArrayList<File>(mainFiles);
        fileResult.addAll(dataFiles);

        return fileResult;
    }
    
    protected static List<String> getFileNames(List<File> files) {
        return Lists.newArrayList(Collections2.transform(files, new Function<File, String>() {
            @Override
            public String apply(File file) {
                return file.getName();
            }
        }));
    }

    protected List<IntentionAction> getAvailableActions() {
        doHighlighting();
        return LightQuickFixTestCase.getAvailableActions(getEditor(), getFile());
    }

    @Override
    public String getName() {
        return "test" + name.replaceFirst(name.substring(0, 1), name.substring(0, 1).toUpperCase());
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/quickfix/" + dataPath + "/";
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    public static boolean isMultiFileName(String fileName) {
        return fileName.contains(MAIN_SUBSTRING) || fileName.contains(DATA_SUBSTRING);
    }

    public static boolean isMainFile(String fileName) {
        return fileName.contains(MAIN_SUBSTRING);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();

        FilenameFilter multifileFileNameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith("before") && JetPsiCheckerMultifileTest.isMainFile(s);
            }
        };

        JetTestCaseBuilder.NamedTestFactory multiFileNamedTestFactory = new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetPsiCheckerMultifileTest(dataPath, name);
            }
        };

        File dir = new File(getTestDataPathBase());
        List<String> subDirs = Arrays.asList(dir.list());
        Collections.sort(subDirs);
        for (String subDirName : subDirs) {
            final TestSuite multiFileTestSuite = JetTestCaseBuilder.suiteForDirectory(getTestDataPathBase(), subDirName, true, multifileFileNameFilter, multiFileNamedTestFactory);
            if (multiFileTestSuite.countTestCases() != 0) {
                suite.addTest(multiFileTestSuite);
            }
        }
        return suite;
    }

    public static String getTestDataPathBase() {
        return JetTestCaseBuilder.getHomeDirectory() + "/idea/testData/quickfix/";
    }
}
