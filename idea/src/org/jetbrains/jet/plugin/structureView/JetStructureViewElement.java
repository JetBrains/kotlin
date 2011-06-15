package org.jetbrains.jet.plugin.structureView;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.NavigatablePsiElement;

import javax.swing.*;

/**
 * @author yole
 */
public class JetStructureViewElement implements StructureViewTreeElement {
    private final NavigatablePsiElement myElement;

    public JetStructureViewElement(NavigatablePsiElement element) {
        myElement = element;
    }

    @Override
    public Object getValue() {
        return myElement;
    }

    @Override
    public void navigate(boolean requestFocus) {
        myElement.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return myElement.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return myElement.canNavigateToSource();
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public String getPresentableText() {
                return "";
            }

            @Override
            public String getLocationString() {
                return null;
            }

            @Override
            public Icon getIcon(boolean open) {
                return myElement.getIcon(open ? Iconable.ICON_FLAG_OPEN : Iconable.ICON_FLAG_CLOSED);
            }

            @Override
            public TextAttributesKey getTextAttributesKey() {
                return null;
            }
        };
    }

    @Override
    public TreeElement[] getChildren() {
        return new TreeElement[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
