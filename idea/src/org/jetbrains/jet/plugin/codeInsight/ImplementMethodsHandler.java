package org.jetbrains.jet.plugin.codeInsight;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.Set;

/**
 * @author yole
 */
public class ImplementMethodsHandler extends OverrideImplementMethodsHandler implements IntentionAction {
    protected Set<CallableMemberDescriptor> collectMethodsToGenerate(MutableClassDescriptor descriptor) {
        Set<CallableMemberDescriptor> missingImplementations = Sets.newLinkedHashSet();
        OverrideResolver.collectMissingImplementations(descriptor, missingImplementations, missingImplementations);
        return missingImplementations;
    }

    protected String getChooserTitle() {
        return "Implement Members";
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("implement.members");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("implement.members");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return isValidFor(editor, file);
    }
}
