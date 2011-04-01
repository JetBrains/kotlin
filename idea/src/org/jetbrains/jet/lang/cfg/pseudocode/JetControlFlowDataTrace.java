package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author abreslav
 */
public class JetControlFlowDataTrace {

    private Map<JetElement, Pseudocode> data = new LinkedHashMap<JetElement, Pseudocode>();

    public void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode) {
        data.put(element, pseudocode);
    }

    @Nullable
    public Pseudocode getControlFlowData(@NotNull JetElement element) {
        return data.get(element);
    }

    public Collection<Pseudocode> getAllData() {
        return data.values();
    }
}
