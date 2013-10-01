package org.jetbrains.jet.plugin.framework.ui;

import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class ChooseLibraryPathPanel {

    private final CopyIntoPanel copyFilePanel;

    private JPanel contentPane;
    private JRadioButton useFromPluginRadioButton;
    private JRadioButton copyToRadioButton;
    private JPanel copyPanelPlace;

    private final EventDispatcher<ActionListener> actionDispatcher = EventDispatcher.create(ActionListener.class);

    public ChooseLibraryPathPanel(@NotNull String defaultPath) {
        ButtonGroup modulesGroup = new ButtonGroup();
        modulesGroup.add(useFromPluginRadioButton);
        modulesGroup.add(copyToRadioButton);
        copyToRadioButton.setSelected(true);

        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionDispatcher.getMulticaster().actionPerformed(e);
                updateComponents();
            }
        };

        copyToRadioButton.addActionListener(actionListener);
        useFromPluginRadioButton.addActionListener(actionListener);

        copyFilePanel = new CopyIntoPanel(null, defaultPath);
        copyFilePanel.addValidityListener(new ValidityListener() {
            @Override
            public void validityChanged(boolean isValid) {
                updateComponents();
            }
        });
        copyPanelPlace.add(copyFilePanel.getContentPane(), BorderLayout.CENTER);

        updateComponents();
    }

    public JComponent getContentPane() {
        return contentPane;
    }

    @Nullable
    public String getPath() {
        return copyToRadioButton.isSelected() ? copyFilePanel.getPath() : null;
    }

    public void addValidityListener(ValidityListener listener) {
        copyFilePanel.addValidityListener(listener);
    }

    public void addActionListener(ActionListener listener) {
        actionDispatcher.addListener(listener);
    }

    public boolean hasErrors() {
        return copyToRadioButton.isSelected() && copyFilePanel.hasErrors();
    }

    private void updateComponents() {
        copyFilePanel.setEnabled(copyToRadioButton.isSelected());
        if (copyToRadioButton.isSelected() && copyFilePanel.hasErrors()) {
            copyToRadioButton.setForeground(Color.RED);
        }
        else {
            copyToRadioButton.setForeground(Color.BLACK);
        }
    }
}
