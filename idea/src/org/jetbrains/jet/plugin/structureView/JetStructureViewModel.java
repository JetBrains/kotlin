package org.jetbrains.jet.plugin.structureView;

import com.intellij.ide.structureView.StructureViewModelBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;

/**
 * @author yole
 */
public class JetStructureViewModel extends StructureViewModelBase {
    public JetStructureViewModel(@NotNull JetFile jetFile) {
        super(jetFile, new JetStructureViewElement(jetFile));
        withSuitableClasses(JetDeclaration.class);
    }
}
