package org.jetbrains.jet.plugin.codeInsight;

import com.google.common.collect.Sets;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.OverrideResolver;

import java.util.Set;

/**
 * @author yole
 */
public class ImplementMethodsHandler extends OverrideImplementMethodsHandler {
    protected Set<CallableMemberDescriptor> collectMethodsToGenerate(MutableClassDescriptor descriptor) {
        Set<CallableMemberDescriptor> missingImplementations = Sets.newLinkedHashSet();
        OverrideResolver.collectMissingImplementations(descriptor, missingImplementations, missingImplementations);
        return missingImplementations;
    }

    protected String getChooserTitle() {
        return "Implement Members";
    }
}
