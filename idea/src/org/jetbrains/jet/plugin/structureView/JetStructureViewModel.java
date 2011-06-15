package org.jetbrains.jet.plugin.structureView;

import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;

/**
 * @author yole
 */
public class JetStructureViewModel extends StructureViewModelBase {
    public JetStructureViewModel(@NotNull PsiFile psiFile) {
        super(psiFile, new JetStructureViewElement(psiFile));
        withSuitableClasses(JetDeclaration.class);
    }
}
