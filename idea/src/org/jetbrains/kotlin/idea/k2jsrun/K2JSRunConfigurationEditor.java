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

package org.jetbrains.kotlin.idea.k2jsrun;

    import com.intellij.ide.browsers.BrowserFamily;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;

public final class K2JSRunConfigurationEditor extends SettingsEditor<K2JSRunConfiguration> {

    private JPanel mainPanel;
    private TextFieldWithBrowseButton htmlChooseFile;
    private JComboBox browserComboBox;
    private JCheckBox openInBrowserCheckBox;
    private TextFieldWithBrowseButton generatedChooseFile;
    private JLabel chooseBrowserLabel;
    private JLabel htmlFileLabel;
    @NotNull
    private final Project project;

    public K2JSRunConfigurationEditor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    protected void resetEditorFrom(@NotNull K2JSRunConfiguration configuration) {
        htmlChooseFile.setText(toSystemIndependentName(configuration.settings().getPageToOpenFilePath()));
        browserComboBox.setSelectedItem(configuration.settings().getBrowserFamily());
        generatedChooseFile.setText(toSystemIndependentName(configuration.settings().getGeneratedFilePath()));
        openInBrowserCheckBox.setSelected(configuration.settings().isShouldOpenInBrowserAfterTranslation());
    }

    @Override
    protected void applyEditorTo(@NotNull K2JSRunConfiguration configuration) throws ConfigurationException {
        K2JSConfigurationSettings settings = configuration.settings();
        settings.setPageToOpenFilePath(toSystemIndependentName(htmlChooseFile.getText()));
        Object item = browserComboBox.getSelectedItem();
        if (item instanceof BrowserFamily) {
            settings.setBrowserFamily((BrowserFamily) item);
        }
        settings.setGeneratedFilePath(toSystemIndependentName(generatedChooseFile.getText()));
        settings.setShouldOpenInBrowserAfterTranslation(openInBrowserCheckBox.isSelected());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        setUpShowInBrowserCheckBox();
        setUpChooseHtmlToShow();
        setUpBrowserCombobox();
        setUpChooseGenerateFilePath();
        return mainPanel;
    }

    private void setUpChooseGenerateFilePath() {
        FileChooserDescriptor fileChooserDescriptor =
            FileChooserDescriptorFactory.getDirectoryChooserDescriptor("directory where generated files will be stored");
        fileChooserDescriptor.setRoots(ProjectRootManager.getInstance(project).getContentRoots());
        generatedChooseFile.addBrowseFolderListener(null, null, project, fileChooserDescriptor);
        final JTextField textField = generatedChooseFile.getTextField();
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(@NotNull DocumentEvent e) {
                onChange();
            }

            @Override
            public void removeUpdate(@NotNull DocumentEvent e) {
                onChange();
            }

            @Override
            public void changedUpdate(@NotNull DocumentEvent e) {
                onChange();
            }

            private void onChange() {
                File file = new File(generatedChooseFile.getText());
                if (!file.isDirectory()) {
                    textField.setForeground(JBColor.red);
                } else {
                    textField.setForeground(JBColor.foreground());
                }
            }
        });
    }

    private void setUpShowInBrowserCheckBox() {
        openInBrowserCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(@NotNull ItemEvent e) {
                boolean selected = openInBrowserCheckBox.isSelected();
                htmlChooseFile.setEnabled(selected);
                browserComboBox.setEnabled(selected);
                htmlFileLabel.setEnabled(selected);
                chooseBrowserLabel.setEnabled(selected);
            }
        });
    }

    private void setUpChooseHtmlToShow() {
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(StdFileTypes.HTML);
        fileChooserDescriptor.setRoots(ProjectRootManager.getInstance(project).getContentRoots());
        htmlChooseFile.addBrowseFolderListener("Choose file to show after translation is finished", null, project, fileChooserDescriptor);
    }

    private void setUpBrowserCombobox() {
        for (WebBrowser browser : WebBrowserManager.getInstance().getActiveBrowsers()) {
            browserComboBox.addItem(browser.getFamily());
        }
        browserComboBox.setRenderer(new ListCellRendererWrapper<BrowserFamily>() {
            @Override
            public void customize(JList list, BrowserFamily family, int index, boolean selected, boolean hasFocus) {
                if (family != null) {
                    setText(family.getName());
                    setIcon(family.getIcon());
                }
            }
        });
        if (browserComboBox.getItemCount() < 2) {
            browserComboBox.setVisible(false);
            browserComboBox.setVisible(false);
        }
        else {
            browserComboBox.setSelectedItem(0);
        }
    }

    @Override
    protected void disposeEditor() {
        // do nothing
    }
}
