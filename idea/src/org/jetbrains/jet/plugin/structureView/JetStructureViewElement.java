package org.jetbrains.jet.plugin.structureView;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.util.PsiIconUtil;
import org.jetbrains.jet.lang.psi.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

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
                String name = myElement.getName();
                if (StringUtil.isEmpty(name)) {
                    if (myElement instanceof JetClassInitializer) {
                        return "<class initializer>";
                    }
                }
                return name;
            }

            @Override
            public String getLocationString() {
                return null;
            }

            @Override
            public Icon getIcon(boolean open) {
                return myElement.isValid()
                        ? PsiIconUtil.getProvidersIcon(myElement, open ? Iconable.ICON_FLAG_OPEN : Iconable.ICON_FLAG_CLOSED)
                        : null;
            }

            @Override
            public TextAttributesKey getTextAttributesKey() {
                return null;
            }
        };
    }

    @Override
    public TreeElement[] getChildren() {
        if (myElement instanceof JetFile) {
            JetNamespace rootNamespace = ((JetFile) myElement).getRootNamespace();
            return new TreeElement[] { new JetStructureViewElement((rootNamespace)) };
        }
        else if (myElement instanceof JetNamespace) {
            return wrapDeclarations(((JetNamespace) myElement).getDeclarations());
        }
        else if (myElement instanceof JetClass) {
            JetClass jetClass = (JetClass) myElement;
            List<JetDeclaration> declarations = new ArrayList<JetDeclaration>();
            for (JetParameter parameter : jetClass.getPrimaryConstructorParameters()) {
                if (parameter.getValOrVarNode() != null) {
                    declarations.add(parameter);
                }
            }
            declarations.addAll(jetClass.getDeclarations());
            return wrapDeclarations(declarations);

        }
        else if (myElement instanceof JetClassOrObject) {
            return wrapDeclarations(((JetClassOrObject) myElement).getDeclarations());
        }
        return new TreeElement[0];
    }

    private static TreeElement[] wrapDeclarations(List<JetDeclaration> declarations) {
        TreeElement[] result = new TreeElement[declarations.size()];
        for (int i = 0; i < declarations.size(); i++) {
            result[i]  = new JetStructureViewElement(declarations.get(i));
        }
        return result;
    }
}
