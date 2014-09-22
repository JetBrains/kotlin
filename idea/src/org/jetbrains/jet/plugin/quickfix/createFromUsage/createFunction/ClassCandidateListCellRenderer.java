package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction;

import com.intellij.ide.util.PsiElementListCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.plugin.presentation.JetClassPresenter;

import javax.swing.*;
import java.awt.*;

class ClassCandidateListCellRenderer extends PsiElementListCellRenderer<JetClass> {
    private final JetClassPresenter presenter = new JetClassPresenter();

    @Override
    public String getElementText(@NotNull JetClass element) {
        return presenter.getPresentation(element).getPresentableText();
    }

    @Nullable
    @Override
    protected String getContainerText(JetClass element, String name) {
        return presenter.getPresentation(element).getLocationString();
    }

    @Override
    protected int getIconFlags() {
        return 0;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        return super.getListCellRendererComponent(list, ((ClassCandidate) value).getJetClass(), index, isSelected, cellHasFocus);
    }
}
