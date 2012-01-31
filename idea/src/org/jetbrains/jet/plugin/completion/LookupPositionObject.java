package org.jetbrains.jet.plugin.completion;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * A hack decision to avoid double completion from different sources.
 *
 * @author Nikolay Krasko
 */
public class LookupPositionObject {
    private final Pair<String, Integer> position;

    public LookupPositionObject(@NotNull PsiElement psiElement) {

        PsiFile file = psiElement.getContainingFile();
        String fileName = file != null ? file.getName() : null;
        
        position = new Pair<String, Integer>(fileName, psiElement.getTextOffset());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }

        return Comparing.equal(position, ((LookupPositionObject) obj).position);
    }

    public int hashCode() {
        return position.hashCode();
    }
}
