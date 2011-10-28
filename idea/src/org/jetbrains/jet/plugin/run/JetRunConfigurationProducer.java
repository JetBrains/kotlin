package org.jetbrains.jet.plugin.run;

import com.intellij.execution.Location;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetMainDetector;

/**
 * @author yole
 */
public class JetRunConfigurationProducer extends RuntimeConfigurationProducer implements Cloneable {
    private PsiElement mySourceElement;

    public JetRunConfigurationProducer() {
        super(JetRunConfigurationType.getInstance());
    }

    @Override
    public PsiElement getSourceElement() {
        return mySourceElement;
    }

    @Override
    protected RunnerAndConfigurationSettings createConfigurationByElement(Location location, ConfigurationContext configurationContext) {
        JetClass containingClass = (JetClass) location.getParentElement(JetClass.class);
        if (containingClass != null && JetMainDetector.hasMain(containingClass.getDeclarations())) {
            mySourceElement = containingClass;
            return createConfigurationByQName(location.getModule(), configurationContext, containingClass.getFQName());
        }
        PsiFile psiFile = location.getPsiElement().getContainingFile();
        if (psiFile instanceof JetFile) {
            JetNamespace namespace = ((JetFile) psiFile).getRootNamespace();
            if (JetMainDetector.hasMain(namespace.getDeclarations())) {
                mySourceElement = namespace;
                return createConfigurationByQName(location.getModule(), configurationContext, namespace.getFQName() + ".namespace");
            }
        }
        return null;
    }

    private RunnerAndConfigurationSettings createConfigurationByQName(Module module, ConfigurationContext context, String fqName) {
        RunnerAndConfigurationSettings settings = cloneTemplateConfiguration(module.getProject(), context);
        JetRunConfiguration configuration = (JetRunConfiguration) settings.getConfiguration();
        configuration.setModule(module);
        configuration.setName(StringUtil.trimEnd(fqName, ".namespace"));
        configuration.MAIN_CLASS_NAME = fqName;
        return settings;
    }

    @Override
    public int compareTo(Object o) {
        return PREFERED;
    }
}
