/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui;

import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ui.AsyncProcessIcon;

import javax.swing.*;

public class ConfigurePluginUpdatesForm {
    public JComboBox<String> channelCombo;
    public JButton reCheckButton;
    public JPanel mainPanel;
    public AsyncProcessIcon updateCheckProgressIcon;
    public JLabel updateStatusLabel;
    public JButton installButton;
    public JLabel installStatusLabel;
    private JLabel verifierDisabledText;

    public ConfigurePluginUpdatesForm() {
        int size = channelCombo.getModel().getSize();
        String maxLengthItem = "";
        for (int i = 0; i < size; i++) {
            String item = channelCombo.getModel().getElementAt(i);
            if (item.length() > maxLengthItem.length()) {
                maxLengthItem = item;
            }
        }
        channelCombo.setPrototypeDisplayValue(maxLengthItem + " ");
        showVerifierDisabledStatus();
    }

    private void createUIComponents() {
        updateCheckProgressIcon = new AsyncProcessIcon("Plugin update check progress");
    }

    public void resetUpdateStatus() {
        updateStatusLabel.setText(" ");
        installButton.setVisible(false);
        installStatusLabel.setVisible(false);
    }

    public void setUpdateStatus(String message, boolean showInstallButton) {
        installButton.setEnabled(true);
        installButton.setVisible(showInstallButton);

        updateStatusLabel.setText(message);

        installStatusLabel.setVisible(true);
        installStatusLabel.setText("");
    }

    public void showInstallButton() {
        installButton.setEnabled(true);
        installButton.setVisible(true);
    }

    public void hideInstallButton() {
        installButton.setEnabled(false);
        installButton.setVisible(false);
    }

    private void showVerifierDisabledStatus() {
        //noinspection UnresolvedPropertyKey
        if (!Registry.is("kotlin.plugin.update.verifier.enabled", true)) {
            verifierDisabledText.setText("(verifier disabled)");
        }
        else {
            verifierDisabledText.setText("");
        }
    }
}
