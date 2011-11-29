package org.jetbrains.jet.plugin;

import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetTypeReference;

import java.util.List;

/**
 * @author yole
 */
public class JetMainDetector {
    private JetMainDetector() {
    }

    public static boolean hasMain(List<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            if (declaration instanceof JetNamedFunction) {
                if (isMain((JetNamedFunction) declaration)) return true;
            }
        }
        return false;
    }

    public static boolean isMain(JetNamedFunction function) {
        if ("main".equals(function.getName())) {
            List<JetParameter> parameters = function.getValueParameters();
            if (parameters.size() == 1) {
                JetTypeReference reference = parameters.get(0).getTypeReference();
                if (reference != null && reference.getText().equals("Array<String>")) {  // TODO correct check
                    return true;
                }
            }
        }
        return false;
    }
}
