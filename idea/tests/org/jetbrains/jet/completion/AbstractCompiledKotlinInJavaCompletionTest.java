package org.jetbrains.jet.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JdkAndMockLibraryProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.project.TargetPlatform;

public abstract class AbstractCompiledKotlinInJavaCompletionTest extends JetFixtureCompletionBaseTestCase {
    @Override
    public TargetPlatform getPlatform() {
        return TargetPlatform.JVM;
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/completion/injava/mockLib", false);
    }

    @NotNull
    @Override
    protected CompletionType completionType() {
        return CompletionType.BASIC;
    }
}
