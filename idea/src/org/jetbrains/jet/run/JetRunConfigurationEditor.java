package org.jetbrains.jet.run;

import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public class JetRunConfigurationEditor extends SettingsEditor<JetRunConfiguration> {
    private JPanel myMainPanel;
    private JTextField myMainClassField;
    private JPanel myModuleChooserHolder;
    private ConfigurationModuleSelector myModuleSelector;

    public JetRunConfigurationEditor(final Project project) {
        LabeledComponent<JComboBox> moduleChooser = LabeledComponent.create(new JComboBox(),  "Use classpath and JDK of module:");
        myModuleChooserHolder.add(moduleChooser, BorderLayout.CENTER);
        myModuleSelector = new ConfigurationModuleSelector(project, moduleChooser.getComponent());
    }

    @Override
    protected void resetEditorFrom(JetRunConfiguration configuration) {
        myMainClassField.setText(configuration.MAIN_CLASS_NAME);
        myModuleSelector.reset(configuration);
    }

    @Override
    protected void applyEditorTo(JetRunConfiguration configuration) throws ConfigurationException {
        myModuleSelector.applyTo(configuration);
        configuration.MAIN_CLASS_NAME = myMainClassField.getText();
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myMainPanel;
    }

    @Override
    protected void disposeEditor() {
    }
}
