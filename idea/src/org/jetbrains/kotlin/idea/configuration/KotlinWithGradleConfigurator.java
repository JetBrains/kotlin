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
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.roots.ModuleRootManager;
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
import org.jetbrains.kotlin.idea.KotlinPluginUtil;
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class KotlinWithGradleConfigurator implements KotlinProjectConfigurator {
    private static final String VERSION_TEMPLATE = "$VERSION$";

    protected static final String CLASSPATH = "classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version\"";

    protected static final String SNAPSHOT_REPOSITORY = "maven {\nurl '" + ConfigureKotlinInProjectUtilsKt.SNAPSHOT_REPOSITORY.getUrl() + "'\n}";
    protected static final String EAP_REPOSITORY = "maven {\nurl '" + ConfigureKotlinInProjectUtilsKt.EAP_REPOSITORY.getUrl() + "'\n}";

    private static final String MAVEN_CENTRAL = "mavenCentral()\n";
    private static final String JCENTER = "jcenter()\n";
    public static final String LIBRARY = "compile \"org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version\"";
    protected static final String SOURCE_SET = "main.java.srcDirs += 'src/main/kotlin'\n";
    private static final String VERSION = String.format("ext.kotlin_version = '%s'", VERSION_TEMPLATE);

    @Override
    public boolean isConfigured(@NotNull Module module) {
        if (ConfigureKotlinInProjectUtilsKt.hasKotlinRuntimeInScope(module)) {
            return true;
        }

        GroovyFile moduleGradleFile = getBuildGradleFile(module.getProject(), getModuleFilePath(module));
        if (moduleGradleFile != null && isFileConfigured(moduleGradleFile)) {
            return true;
        }
        GroovyFile projectGradleFile = getBuildGradleFile(module.getProject(), getTopLevelProjectFilePath(module.getProject()));
        return projectGradleFile != null && isFileConfigured(projectGradleFile);
    }

    private boolean isFileConfigured(GroovyFile projectGradleFile) {
        String fileText = projectGradleFile.getText();
        return containsDirective(fileText, getApplyPluginDirective()) && fileText.contains(LIBRARY);
    }

    private static boolean containsDirective(@NotNull String fileText, @NotNull String directive) {
        return fileText.contains(directive)
               || fileText.contains(directive.replace("\"", "'"))
               || fileText.contains(directive.replace("'", "\""));
    }

    @Override
    public void configure(@NotNull final Project project, Collection<Module> excludeModules) {
        final ConfigureDialogWithModulesAndVersion dialog =
                new ConfigureDialogWithModulesAndVersion(project, this, excludeModules);

        dialog.show();
        if (!dialog.isOK()) return;

        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                NotificationMessageCollector collector = NotificationMessageCollectorKt.createConfigureKotlinNotificationCollector(project);
                Set<GroovyFile> changedFiles = new HashSet<GroovyFile>();
                GroovyFile projectGradleFile = getBuildGradleFile(project, getTopLevelProjectFilePath(project));
                if (projectGradleFile != null && canConfigureFile(projectGradleFile)) {
                    boolean isModified = changeGradleFile(projectGradleFile, true, dialog.getKotlinVersion(), collector);
                    if (isModified) {
                        changedFiles.add(projectGradleFile);
                    }
                }

                for (Module module : dialog.getModulesToConfigure()) {
                    GroovyFile file = getBuildGradleFile(project, getModuleFilePath(module));
                    if (file != null && canConfigureFile(file)) {
                        boolean isModified = changeGradleFile(file, false, dialog.getKotlinVersion(), collector);
                        if (isModified) {
                            changedFiles.add(file);
                        }
                    }
                    else {
                        showErrorMessage(project, "Cannot find build.gradle file for module " + module.getName());
                    }
                }

                for (GroovyFile file : changedFiles) {
                    OpenFileAction.openFile(file.getVirtualFile(), project);
                }
                collector.showNotification();
            }
        }, "Configure Kotlin", null);
    }

    public static void addKotlinLibraryToModule(final Module module, final DependencyScope scope, final ExternalLibraryDescriptor libraryDescriptor) {
        String gradleFilePath = getModuleFilePath(module);
        final GroovyFile gradleFile = getBuildGradleFile(module.getProject(), gradleFilePath);

        if (gradleFile != null && canConfigureFile(gradleFile)) {
            new WriteCommandAction(gradleFile.getProject()) {
                @Override
                protected void run(@NotNull Result result) {
                    String groovyScope;
                    switch (scope) {
                        case COMPILE:
                            groovyScope = "compile";
                            break;
                        case TEST:
                            if (KotlinPluginUtil.isAndroidGradleModule(module)) {
                                // TODO we should add testCompile or androidTestCompile
                                groovyScope = "compile";
                            }
                            else {
                                groovyScope = "testCompile";
                            }
                            break;
                        case RUNTIME:
                            groovyScope = "runtime";
                            break;
                        case PROVIDED:
                            groovyScope = "compile";
                            break;
                        default:
                            groovyScope = "compile";
                    }

                    String dependencyString = String.format(
                            "%s \"%s:%s:%s\"",
                            groovyScope, libraryDescriptor.getLibraryGroupId(), libraryDescriptor.getLibraryArtifactId(),
                            libraryDescriptor.getMaxVersion());

                    GrClosableBlock dependenciesBlock = getDependenciesBlock(gradleFile);
                    addLastExpressionInBlockIfNeeded(dependencyString, dependenciesBlock);

                    CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(gradleFile);
                }
            }.execute();

            VirtualFile virtualFile = gradleFile.getVirtualFile();
            if (virtualFile != null) {
                NotificationMessageCollectorKt.createConfigureKotlinNotificationCollector(gradleFile.getProject())
                        .addMessage(virtualFile.getPath() + " was modified")
                        .showNotification();
            }
        }
    }

    @Nullable
    public static String getKotlinStdlibVersion(@NotNull Module module) {
        String gradleFilePath = getModuleFilePath(module);
        GroovyFile gradleFile = getBuildGradleFile(module.getProject(), gradleFilePath);

        if (gradleFile == null) return null;

        String versionProperty = "$kotlin_version";
        GrClosableBlock block = getBuildScriptBlock(gradleFile);
        if (block.getText().contains("ext.kotlin_version = ")) {
            return versionProperty;
        }

        GrStatement[] dependencies = getDependenciesBlock(gradleFile).getStatements();
        String stdlibArtifactPrefix = "org.jetbrains.kotlin:kotlin-stdlib:";
        for (GrStatement dependency : dependencies) {
            String dependencyText = dependency.getText();
            int startIndex = dependencyText.indexOf(stdlibArtifactPrefix) + stdlibArtifactPrefix.length();
            int endIndex = dependencyText.length() - 1;
            if (startIndex != -1 && endIndex != -1) {
                return dependencyText.substring(startIndex, endIndex);
            }
        }

        return null;
    }

    protected static boolean addElementsToProjectFile(@NotNull GroovyFile file, @NotNull String version) {
        boolean wasModified;

        GrClosableBlock buildScriptBlock = getBuildScriptBlock(file);
        wasModified = addFirstExpressionInBlockIfNeeded(VERSION.replace(VERSION_TEMPLATE, version), buildScriptBlock);

        GrClosableBlock buildScriptRepositoriesBlock = getBuildScriptRepositoriesBlock(file);
        if (ConfigureKotlinInProjectUtilsKt.isSnapshot(version)) {
            wasModified |= addLastExpressionInBlockIfNeeded(SNAPSHOT_REPOSITORY, buildScriptRepositoriesBlock);
        }
        else if (ConfigureKotlinInProjectUtilsKt.isEap(version)) {
            wasModified |= addLastExpressionInBlockIfNeeded(EAP_REPOSITORY, buildScriptRepositoriesBlock);
        }
        else if (!isRepositoryConfigured(buildScriptRepositoriesBlock)) {
            wasModified |= addLastExpressionInBlockIfNeeded(MAVEN_CENTRAL, buildScriptRepositoriesBlock);
        }

        GrClosableBlock buildScriptDependenciesBlock = getBuildScriptDependenciesBlock(file);
        wasModified |= addLastExpressionInBlockIfNeeded(CLASSPATH, buildScriptDependenciesBlock);

        return wasModified;
    }

    protected boolean addElementsToModuleFile(@NotNull GroovyFile file, @NotNull String version) {
        boolean wasModified = false;

        if (!containsDirective(file.getText(), getApplyPluginDirective())) {
            GrExpression apply = GroovyPsiElementFactory.getInstance(file.getProject()).createExpressionFromText(getApplyPluginDirective());
            GrApplicationStatement applyStatement = getApplyStatement(file);
            if (applyStatement != null) {
                file.addAfter(apply, applyStatement);
                wasModified = true;
            }
            else {
                GrClosableBlock buildScript = getBlockByName(file, "buildscript");
                if (buildScript != null) {
                    file.addAfter(apply, buildScript.getParent());
                    wasModified = true;
                }
                else {
                    GrStatement[] statements = file.getStatements();
                    if (statements.length > 0) {
                        file.addAfter(apply, statements[statements.length - 1]);
                    }
                    else {
                        file.addAfter(apply, file.getFirstChild());
                    }
                    wasModified = true;
                }
            }
        }

        GrClosableBlock repositoriesBlock = getRepositoriesBlock(file);
        if (ConfigureKotlinInProjectUtilsKt.isSnapshot(version)) {
            wasModified |= addLastExpressionInBlockIfNeeded(SNAPSHOT_REPOSITORY, repositoriesBlock);
        }
        else if (ConfigureKotlinInProjectUtilsKt.isEap(version)) {
            wasModified |= addLastExpressionInBlockIfNeeded(EAP_REPOSITORY, repositoriesBlock);
        }
        else if (!isRepositoryConfigured(repositoriesBlock)) {
            wasModified |= addLastExpressionInBlockIfNeeded(MAVEN_CENTRAL, repositoriesBlock);
        }

        GrClosableBlock dependenciesBlock = getDependenciesBlock(file);
        wasModified |= addExpressionInBlockIfNeeded(LIBRARY, dependenciesBlock, false);

        wasModified |= addSourceSetsBlock(file);

        return wasModified;
    }

    private static boolean isRepositoryConfigured(GrClosableBlock repositoriesBlock) {
        return repositoriesBlock.getText().contains(MAVEN_CENTRAL) || repositoriesBlock.getText().contains(JCENTER);
    }

    protected abstract String getApplyPluginDirective();

    protected abstract boolean addSourceSetsBlock(@NotNull GroovyFile file);

    protected abstract boolean addElementsToFile(
            @NotNull GroovyFile groovyFile,
            boolean isTopLevelProjectFile,
            @NotNull String version
    );

    private static boolean canConfigureFile(@NotNull GroovyFile file) {
        return WritingAccessProvider.isPotentiallyWritable(file.getVirtualFile(), null);
    }

    @Nullable
    private static GroovyFile getBuildGradleFile(Project project, @Nullable String path) {
        if (path == null) {
            return null;
        }
        VirtualFile file = VfsUtil.findFileByIoFile(new File(path), true);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (!(psiFile instanceof GroovyFile)) return null;
        return (GroovyFile) psiFile;
    }

    @NotNull
    private static String getTopLevelProjectFilePath(@NotNull Project project) {
        return project.getBasePath() + "/" + GradleConstants.DEFAULT_SCRIPT_NAME;
    }

    @Nullable
    private static String getModuleFilePath(@NotNull Module module) {
        String moduleDir = new File(module.getModuleFilePath()).getParent();
        File buildGradleFile = new File(moduleDir + "/" + GradleConstants.DEFAULT_SCRIPT_NAME);
        if (buildGradleFile.exists()) {
            return buildGradleFile.getPath();
        }

        // since IDEA 145 module file is located in .idea directory
        for (VirtualFile file : ModuleRootManager.getInstance(module).getContentRoots()) {
            buildGradleFile = new File(file.getPath() + "/" + GradleConstants.DEFAULT_SCRIPT_NAME);
            if (buildGradleFile.exists()) {
                return buildGradleFile.getPath();
            }
        }
        return null;
    }

    protected boolean changeGradleFile(
            @NotNull final GroovyFile groovyFile,
            final boolean isTopLevelProjectFile,
            @NotNull final String version,
            @NotNull NotificationMessageCollector collector
    ) {
        final boolean[] isModified = {false};
        new WriteCommandAction(groovyFile.getProject()) {
            @Override
            protected void run(@NotNull Result result) {
                isModified[0] = addElementsToFile(groovyFile, isTopLevelProjectFile, version);

                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(groovyFile);
            }
        }.execute();

        VirtualFile virtualFile = groovyFile.getVirtualFile();
        if (virtualFile != null && isModified[0]) {
            collector.addMessage(virtualFile.getPath() + " was modified");
        }
        return isModified[0];
    }

    @NotNull
    private static GrClosableBlock getDependenciesBlock(@NotNull GrStatementOwner file) {
        return getBlockOrCreate(file, "dependencies");
    }

    @NotNull
    protected static GrClosableBlock getSourceSetsBlock(@NotNull GrStatementOwner parent) {
        return getBlockOrCreate(parent, "sourceSets");
    }

    @NotNull
    private static GrClosableBlock getBuildScriptBlock(@NotNull GrStatementOwner file) {
        return getBlockOrCreate(file, "buildscript");
    }

    @NotNull
    private static GrClosableBlock getBuildScriptDependenciesBlock(@NotNull GrStatementOwner file) {
        GrClosableBlock buildScript = getBuildScriptBlock(file);
        return getBlockOrCreate(buildScript, "dependencies");
    }

    @NotNull
    private static GrClosableBlock getBuildScriptRepositoriesBlock(@NotNull GrStatementOwner file) {
        GrClosableBlock buildScript = getBuildScriptBlock(file);
        return getBlockOrCreate(buildScript, "repositories");
    }

    @NotNull
    private static GrClosableBlock getRepositoriesBlock(@NotNull GrStatementOwner file) {
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

    protected static boolean addLastExpressionInBlockIfNeeded(@NotNull String text, @NotNull GrClosableBlock block) {
        return addExpressionInBlockIfNeeded(text, block, false);
    }

    private static boolean addFirstExpressionInBlockIfNeeded(@NotNull String text, @NotNull GrClosableBlock block) {
        return addExpressionInBlockIfNeeded(text, block, true);
    }

    @Nullable
    private static GrClosableBlock getBlockByName(@NotNull PsiElement parent, @NotNull String name) {
        GrMethodCallExpression[] allExpressions = PsiTreeUtil.getChildrenOfType(parent, GrMethodCallExpression.class);
        if (allExpressions != null) {
            for (GrMethodCallExpression expression : allExpressions) {
                GrExpression invokedExpression = expression.getInvokedExpression();
                if (expression.getClosureArguments().length == 0) continue;

                String expressionText = invokedExpression.getText();
                if (expressionText.equals(name)) return expression.getClosureArguments()[0];
            }
        }
        return null;
    }

    private static boolean addExpressionInBlockIfNeeded(@NotNull String text, @NotNull GrClosableBlock block, boolean isFirst) {
        if (block.getText().contains(text)) return false;
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
        return true;
    }

    @Nullable
    private static GrApplicationStatement getApplyStatement(@NotNull GroovyFile file) {
        GrApplicationStatement[] applyStatement = PsiTreeUtil.getChildrenOfType(file, GrApplicationStatement.class);
        if (applyStatement == null) return null;
        for (GrApplicationStatement callExpression : applyStatement) {
            GrExpression invokedExpression = callExpression.getInvokedExpression();
            if (invokedExpression.getText().equals("apply")) {
                return callExpression;
            }
        }
        return null;
    }

    private static void showErrorMessage(@NotNull Project project, @Nullable String message) {
        Messages.showErrorDialog(project,
                                 "<html>Couldn't configure kotlin-gradle plugin automatically.<br/>" +
                                 (message != null ? message : "") +
                                 "See manual installation instructions <a href=\"https://kotlinlang.org/docs/reference/using-gradle.html\">here</a></html>",
                                 "Configure Kotlin-Gradle Plugin");
    }
}
