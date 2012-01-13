package org.jetbrains.jet.plugin.structureView;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;

/**
 * @author yole
 */
public class JetStructureViewFactory implements PsiStructureViewFactory {
    @Override
    public StructureViewBuilder getStructureViewBuilder(final PsiFile psiFile) {
        if (psiFile instanceof JetFile) {
            final JetFile file = (JetFile) psiFile;

            return new TreeBasedStructureViewBuilder() {
                @NotNull
                @Override
                public StructureViewModel createStructureViewModel() {
                    return new JetStructureViewModel(file);
                }
            };
        }

        return null;
    }
}
