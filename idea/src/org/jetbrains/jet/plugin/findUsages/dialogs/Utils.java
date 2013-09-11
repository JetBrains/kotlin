package org.jetbrains.jet.plugin.findUsages.dialogs;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

class Utils {
    private Utils() {
    }

    static boolean renameCheckbox(@NotNull JPanel panel, @NotNull String srcText, @NotNull String destText) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if (checkBox.getText().equals(srcText)) {
                    checkBox.setText(destText);
                    return true;
                }
            }
        }

        return false;
    }

    static boolean removeCheckbox(@NotNull JPanel panel, @NotNull String srcText) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if (checkBox.getText().equals(srcText)) {
                    panel.remove(checkBox);
                    return true;
                }
            }
        }

        return false;
    }
}
