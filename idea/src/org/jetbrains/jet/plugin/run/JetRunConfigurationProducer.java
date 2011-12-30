package org.jetbrains.jet.plugin.run;

import com.intellij.execution.Location;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.plugin.JetMainDetector;

/**
 * @author yole
 */
public class JetRunConfigurationProducer extends RuntimeConfigurationProducer implements Cloneable {
    @Nullable
    private PsiElement mySourceElement;

    public JetRunConfigurationProducer() {
        super(JetRunConfigurationType.getInstance());
    }

    @Nullable
    @Override
    public PsiElement getSourceElement() {
        return mySourceElement;
    }

    @Override
    protected RunnerAndConfigurationSettings createConfigurationByElement(@NotNull Location location, ConfigurationContext configurationContext) {
        final Module module = location.getModule();
        if (module == null) {
            return null;
        }

        PsiFile psiFile = location.getPsiElement().getContainingFile();
        if (psiFile instanceof JetFile) {
            JetFile jetFile = (JetFile) psiFile;
            if (JetMainDetector.hasMain(jetFile.getDeclarations())) {
                mySourceElement = jetFile;
                String fqName = JetPsiUtil.getFQName(jetFile);
                String className = fqName.length() == 0 ? JvmAbi.PACKAGE_CLASS : fqName + "." + JvmAbi.PACKAGE_CLASS;
                return createConfigurationByQName(module, configurationContext, className);
            }
        }
        return null;
    }

    private RunnerAndConfigurationSettings createConfigurationByQName(
            @NotNull Module module,
            ConfigurationContext context,
            @NotNull String fqName
    ) {
        RunnerAndConfigurationSettings settings = cloneTemplateConfiguration(module.getProject(), context);
        JetRunConfiguration configuration = (JetRunConfiguration) settings.getConfiguration();
        configuration.setModule(module);
        configuration.setName(StringUtil.trimEnd(fqName, "." + JvmAbi.PACKAGE_CLASS));
        configuration.MAIN_CLASS_NAME = fqName;
        return settings;
    }

    @Override
    public int compareTo(Object o) {
        return PREFERED;
    }
}
