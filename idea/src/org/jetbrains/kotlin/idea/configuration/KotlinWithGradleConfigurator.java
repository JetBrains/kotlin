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

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion;
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrStatementOwner;

import java.io.File;
import java.util.List;

import static org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils.showInfoNotification;

public abstract class KotlinWithGradleConfigurator implements KotlinProjectConfigurator {
    private static final String[] KOTLIN_VERSIONS = {"0.6.+"};

    protected static final String VERSION_TEMPLATE = "$VERSION$";

    protected static final String CLASSPATH = "classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version\"";
    protected static final String SNAPSHOT_REPOSITORY = "maven {\nurl 'http://oss.sonatype.org/content/repositories/snapshots'\n}";
    protected static final String REPOSITORY = "mavenCentral()\n";
    protected static final String LIBRARY = "compile \"org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version\"";
    protected static final String SOURCE_SET = "main.java.srcDirs += 'src/main/kotlin'\n";
    protected static final String VERSION = String.format("ext.kotlin_version = '%s'", VERSION_TEMPLATE);

    @Override
    public boolean isConfigured(@NotNull Module module) {
        if (ProjectStructureUtil.isJavaKotlinModule(module)) {
            return true;
        }

        GroovyFile moduleGradleFile = getBuildGradleFile(module.getProject(), getDefaultPathToBuildGradleFile(module));
        if (moduleGradleFile != null && isFileConfigured(moduleGradleFile)) {
            return true;
        }
        GroovyFile projectGradleFile = getBuildGradleFile(module.getProject(), getDefaultPathToBuildGradleFile(module.getProject()));
        return projectGradleFile != null && isFileConfigured(projectGradleFile);
    }

    private boolean isFileConfigured(GroovyFile projectGradleFile) {
        String fileText = projectGradleFile.getText();
        return fileText.contains(getApplyPluginDirective()) && fileText.contains(LIBRARY);
    }

    @Override
    public void configure(@NotNull Project project) {
        List<Module> nonConfiguredModules = ConfigureKotlinInProjectUtils.getNonConfiguredModules(project, this);

        ConfigureDialogWithModulesAndVersion dialog =
                new ConfigureDialogWithModulesAndVersion(project, nonConfiguredModules, KOTLIN_VERSIONS);

        dialog.show();
        if (!dialog.isOK()) return;

        for (Module module : dialog.getModulesToConfigure()) {
            String gradleFilePath = getDefaultPathToBuildGradleFile(module);
            GroovyFile file = getBuildGradleFile(project, gradleFilePath);
            if (file != null && canConfigureFile(file)) {
                changeGradleFile(file, dialog.getKotlinVersion());
                OpenFileAction.openFile(gradleFilePath, project);
            }
            else {
                showErrorMessage(project, "Cannot find build.gradle file for module " + module.getName());
            }
        }
    }

    private void addElements(@NotNull GroovyFile file, @NotNull String version) {
        GrClosableBlock buildScriptBlock = getBuildScriptBlock(file);
        addFirstExpressionInBlockIfNeeded(VERSION.replace(VERSION_TEMPLATE, version), buildScriptBlock);

        GrClosableBlock buildScriptRepositoriesBlock = getBuildScriptRepositoriesBlock(file);
        if (isSnapshot(version)) {
            addLastExpressionInBlockIfNeeded(SNAPSHOT_REPOSITORY, buildScriptRepositoriesBlock);
        }
        else if (!buildScriptRepositoriesBlock.getText().contains(REPOSITORY)) {
            addLastExpressionInBlockIfNeeded(REPOSITORY, buildScriptRepositoriesBlock);
        }

        GrClosableBlock buildScriptDependenciesBlock = getBuildScriptDependenciesBlock(file);
        addLastExpressionInBlockIfNeeded(CLASSPATH, buildScriptDependenciesBlock);

        if (!file.getText().contains(getApplyPluginDirective())) {
            GrExpression apply = GroovyPsiElementFactory.getInstance(file.getProject()).createExpressionFromText(getApplyPluginDirective());
            GrApplicationStatement applyStatement = getApplyStatement(file);
            if (applyStatement != null) {
                file.addAfter(apply, applyStatement);
            }
            else {
                file.addAfter(apply, buildScriptBlock.getParent());
            }
        }

        GrClosableBlock repositoriesBlock = getRepositoriesBlock(file);
        if (isSnapshot(version)) {
            addLastExpressionInBlockIfNeeded(SNAPSHOT_REPOSITORY, repositoriesBlock);
        }
        else if (!repositoriesBlock.getText().contains(REPOSITORY)) {
            addLastExpressionInBlockIfNeeded(REPOSITORY, repositoriesBlock);
        }

        GrClosableBlock dependenciesBlock = getDependenciesBlock(file);
        addLastExpressionInBlockIfNeeded(LIBRARY, dependenciesBlock);

        addSourceSetsBlock(file);
    }

    protected abstract String getApplyPluginDirective();

    protected abstract void addSourceSetsBlock(@NotNull GroovyFile file);

    protected static boolean canConfigureFile(@NotNull GroovyFile file) {
        return WritingAccessProvider.isPotentiallyWritable(file.getVirtualFile(), null);
    }

    @Nullable
    protected static GroovyFile getBuildGradleFile(Project project, String path) {
        VirtualFile file = VfsUtil.findFileByIoFile(new File(path), true);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (!(psiFile instanceof GroovyFile)) return null;
        return (GroovyFile) psiFile;
    }

    @NotNull
    protected static String getDefaultPathToBuildGradleFile(@NotNull Project project) {
        return project.getBasePath() + "/" + GradleConstants.DEFAULT_SCRIPT_NAME;
    }

    @NotNull
    protected static String getDefaultPathToBuildGradleFile(@NotNull Module module) {
        return new File(module.getModuleFilePath()).getParent() + "/" + GradleConstants.DEFAULT_SCRIPT_NAME;
    }

    protected static boolean isSnapshot(@NotNull String version) {
        return version.contains("SNAPSHOT");
    }

    protected void changeGradleFile(@NotNull final GroovyFile groovyFile, @NotNull final String version) {
        new WriteCommandAction(groovyFile.getProject()) {
            @Override
            protected void run(Result result) {
                addElements(groovyFile, version);

                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(groovyFile);
            }
        }.execute();

        VirtualFile virtualFile = groovyFile.getVirtualFile();
        if (virtualFile != null) {
            showInfoNotification(groovyFile.getProject(), virtualFile.getPath() + " was modified");
        }
    }

    @NotNull
    protected static GrClosableBlock getDependenciesBlock(@NotNull GrStatementOwner file) {
        return getBlockOrCreate(file, "dependencies");
    }

    @NotNull
    protected static GrClosableBlock getSourceSetsBlock(@NotNull GrStatementOwner parent) {
        return getBlockOrCreate(parent, "sourceSets");
    }

    @NotNull
    protected static GrClosableBlock getBuildScriptBlock(@NotNull GrStatementOwner file) {
        return getBlockOrCreate(file, "buildscript");
    }

    @NotNull
    protected static GrClosableBlock getBuildScriptDependenciesBlock(@NotNull GrStatementOwner file) {
        GrClosableBlock buildScript = getBuildScriptBlock(file);
        return getBlockOrCreate(buildScript, "dependencies");
    }

    @NotNull
    protected static GrClosableBlock getBuildScriptRepositoriesBlock(@NotNull GrStatementOwner file) {
        GrClosableBlock buildScript = getBuildScriptBlock(file);
        return getBlockOrCreate(buildScript, "repositories");
    }

    @NotNull
    protected static GrClosableBlock getRepositoriesBlock(@NotNull GrStatementOwner file) {
        return getBlockOrCreate(file, "repositories");
    }

    @NotNull
    protected static GrClosableBlock getBlockOrCreate(@NotNull GrStatementOwner parent, @NotNull String name) {
        GrClosableBlock block = getBlockByName(parent, name);
        if (block == null) {
            GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(parent.getProject());
            GrExpression newBlock = factory.createExpressionFromText(name + "{\n}\n");
            GrStatement[] statements = parent.getStatements();
            if (statements.length > 0) {
                parent.addAfter(newBlock, statements[statements.length - 1]);
            }
            else {
                parent.addAfter(newBlock, parent.getFirstChild());
            }
            block = getBlockByName(parent, name);
            assert block != null : "Block should be non-null because it is created";
        }
        return block;
    }

    protected static void addLastExpressionInBlockIfNeeded(@NotNull String text, @NotNull GrClosableBlock block) {
        addExpressionInBlockIfNeeded(text, block, false);
    }

    protected static void addFirstExpressionInBlockIfNeeded(@NotNull String text, @NotNull GrClosableBlock block) {
        addExpressionInBlockIfNeeded(text, block, true);
    }

    @Nullable
    private static GrClosableBlock getBlockByName(@NotNull PsiElement parent, @NotNull String name) {
        GrMethodCallExpression[] allExpressions = PsiTreeUtil.getChildrenOfType(parent, GrMethodCallExpression.class);
        if (allExpressions != null) {
            for (GrMethodCallExpression expression : allExpressions) {
                GrExpression invokedExpression = expression.getInvokedExpression();
                if (invokedExpression == null) continue;
                if (expression.getClosureArguments().length == 0) continue;

                String expressionText = invokedExpression.getText();
                if (expressionText.equals(name)) return expression.getClosureArguments()[0];
            }
        }
        return null;
    }

    private static void addExpressionInBlockIfNeeded(@NotNull String text, @NotNull GrClosableBlock block, boolean isFirst) {
        if (block.getText().contains(text)) return;
        GrExpression newStatement = GroovyPsiElementFactory.getInstance(block.getProject()).createExpressionFromText(text);
        CodeStyleManager.getInstance(block.getProject()).reformat(newStatement);
        GrStatement[] statements = block.getStatements();
        if (!isFirst && statements.length > 0) {
            GrStatement lastStatement = statements[statements.length - 1];
            if (lastStatement != null) {
                block.addAfter(newStatement, lastStatement);
            }
        }
        else {
            PsiElement firstChild = block.getFirstChild();
            if (firstChild != null) {
                block.addAfter(newStatement, firstChild);
            }
        }
    }

    @Nullable
    private static GrApplicationStatement getApplyStatement(@NotNull GroovyFile file) {
        GrApplicationStatement[] applyStatement = PsiTreeUtil.getChildrenOfType(file, GrApplicationStatement.class);
        if (applyStatement == null) return null;
        for (GrApplicationStatement callExpression : applyStatement) {
            GrExpression invokedExpression = callExpression.getInvokedExpression();
            if (invokedExpression != null && invokedExpression.getText().equals("apply")) {
                return callExpression;
            }
        }
        return null;
    }

    protected static void showErrorMessage(@NotNull Project project, @Nullable String message) {
        Messages.showErrorDialog(project,
                                 "<html>Couldn't configure kotlin-gradle plugin automatically.<br/>" +
                                 (message != null ? message : "") +
                                 "See manual installation instructions <a href=\"http://confluence.jetbrains.com/display/Kotlin/Kotlin+Build+Tools#KotlinBuildTools-Gradle\">here</a></html>",
                                 "Configure Kotlin-Gradle Plugin");
    }
}
