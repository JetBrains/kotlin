package org.jetbrains.jet.plugin.run;

import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author yole
 */
public class JetRunConfigurationEditor extends SettingsEditor<JetRunConfiguration> {
    private JPanel myMainPanel;
    private JTextField myMainClassField;
    private JPanel myModuleChooserHolder;
    private CommonJavaParametersPanel myCommonProgramParameters;
    private ConfigurationModuleSelector myModuleSelector;

    public JetRunConfigurationEditor(final Project project) {
        LabeledComponent<JComboBox> moduleChooser = LabeledComponent.create(new JComboBox(),  "Use classpath and JDK of module:");
        myModuleChooserHolder.add(moduleChooser, BorderLayout.CENTER);
        myModuleSelector = new ConfigurationModuleSelector(project, moduleChooser.getComponent());
        myCommonProgramParameters.setModuleContext(myModuleSelector.getModule());
        moduleChooser.getComponent().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                myCommonProgramParameters.setModuleContext(myModuleSelector.getModule());
            }
        });
    }

    @Override
    protected void resetEditorFrom(JetRunConfiguration configuration) {
        myCommonProgramParameters.reset(configuration);
        myMainClassField.setText(configuration.MAIN_CLASS_NAME);
        myModuleSelector.reset(configuration);
    }

    @Override
    protected void applyEditorTo(JetRunConfiguration configuration) throws ConfigurationException {
        myModuleSelector.applyTo(configuration);
        myCommonProgramParameters.applyTo(configuration);
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
