package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetAttribute;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.types.Attribute;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class AttributeResolver {
    public static final AttributeResolver INSTANCE = new AttributeResolver();

    private AttributeResolver() {}

    @NotNull
    public List<Attribute> resolveAttributes(@NotNull List<JetAttribute> attributeElements) {
        return Collections.emptyList(); // TODO
//        if (attributeElements.isEmpty()) {
//        }
//        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    public List<Attribute> resolveAttributes(@Nullable JetModifierList modifierList) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        return resolveAttributes(modifierList.getAttributes());
    }
}
