package org.jetbrains.jet.plugin.framework.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class CreateLibrarySourceDialog extends DialogWrapper {
    private JPanel contentPane;
    private JRadioButton useStandaloneKotlinRadioButton;
    private JRadioButton useBundledKotlinRadioButton;
    private JCheckBox copyLibraryCheckbox;
    private TextFieldWithBrowseButton copyIntoDirectoryField;
    private TextFieldWithBrowseButton kotlinStandalonePathField;

    private String version = null;

    private final String initialStandaloneLabelText;

    public CreateLibrarySourceDialog(@Nullable Project project, @NotNull String title, VirtualFile contextDirectory) {
        super(project);

        setTitle(title);

        init();

        contentPane.setMinimumSize(new Dimension(380, 180));

        useBundledKotlinRadioButton.setText(useBundledKotlinRadioButton.getText() + " - " + JetPluginUtil.getPluginVersion());

        initialStandaloneLabelText = useStandaloneKotlinRadioButton.getText();

        kotlinStandalonePathField.setEditable(false);
        kotlinStandalonePathField.addBrowseFolderListener(
                "Kotlin Compiler", "Choose folder with Kotlin compiler installation", project,
                new FileChooserDescriptor(false, true, false, false, false, false) {
                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        if (!super.isFileSelectable(file)) {
                            return false;
                        }

                        return PathUtil.KOTLIN_HOME_DIRECTORY_ADAPTER.fun(com.intellij.util.PathUtil.getLocalPath(file)) != null;
                    }
                });

        kotlinStandalonePathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final DocumentEvent e) {
                updateStandaloneVersion();
                updateComponentVersion();
            }
        });

        updateStandaloneVersion();

        copyIntoDirectoryField.addBrowseFolderListener(
                "Copy Into...", "Choose folder where files will be copied", project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor());
        copyIntoDirectoryField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final DocumentEvent e) {
                updateComponentVersion();
            }
        });

        copyIntoDirectoryField.getTextField().setText(FileUIUtils.getDefaultLibraryFolder(project, contextDirectory));

        copyLibraryCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        });
        useStandaloneKotlinRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        });
        useBundledKotlinRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        });

        updateComponents();
    }

    @Nullable
    public String getStandaloneCompilerPath() {
        if (useStandaloneKotlinRadioButton.isSelected()) {
            return kotlinStandalonePathField.getText().trim();
        }

        return null;
    }

    @Nullable
    public String getCopyIntoPath() {
        if (copyLibraryCheckbox.isSelected()) {
            return copyIntoDirectoryField.getText().trim();
        }

        return null;
    }

    @NotNull
    public String getVersion() {
        if (useBundledKotlinRadioButton.isSelected()) {
            return JetPluginUtil.getPluginVersion();
        }
        else {
            assert version != null: "It shouldn't be possible to finish dialog with invalid version";
            return version;
        }
    }

    private void updateStandaloneVersion() {
        if (useStandaloneKotlinRadioButton.isSelected()) {
            KotlinPaths paths = PathUtil.getKotlinStandaloneCompilerPaths(kotlinStandalonePathField.getTextField().getText().trim());
            try {
                version = FileUtilRt.loadFile(paths.getBuildVersionFile());
                return;
            }
            catch (IOException e) {
                // Do nothing. Version will be set to null.
            }
        }

        version = null;
    }

    private void updateComponentVersion() {
        boolean isError = false;

        useStandaloneKotlinRadioButton.setForeground(Color.BLACK);
        useStandaloneKotlinRadioButton.setText(initialStandaloneLabelText);
        if (useStandaloneKotlinRadioButton.isSelected()) {
            if (version != null) {
                useStandaloneKotlinRadioButton.setText(initialStandaloneLabelText + " - " + version);
            }
            else {
                useStandaloneKotlinRadioButton.setForeground(Color.RED);
                useStandaloneKotlinRadioButton.setText(initialStandaloneLabelText + " - " + "Invalid Version");
                isError = true;
            }
        }

        copyLibraryCheckbox.setForeground(Color.BLACK);
        if (copyLibraryCheckbox.isSelected()) {
            if (copyIntoDirectoryField.getText().trim().isEmpty()) {
                copyLibraryCheckbox.setForeground(Color.RED);
                isError = true;
            }
        }

        setOKActionEnabled(!isError);
    }

    private void updateComponents() {
        kotlinStandalonePathField.setEnabled(useStandaloneKotlinRadioButton.isSelected());
        copyIntoDirectoryField.setEnabled(copyLibraryCheckbox.isSelected());
        updateComponentVersion();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
