package org.jetbrains.jet.lang.diagnostics;

import com.google.common.collect.Maps;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.util.slicedmap.MutableSlicedMap;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.SlicedMapImpl;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Map;

/**
 * @author svtk
 */
public class DiagnosticWithParameters<T extends PsiElement> extends DiagnosticWithPsiElementImpl<T> {

    private final Map<DiagnosticParameter, Object> map = Maps.newHashMap();

    public DiagnosticWithParameters(DiagnosticWithPsiElement<T> diagnostic) {
        super(diagnostic.getFactory(), diagnostic.getSeverity(), diagnostic.getMessage(), diagnostic.getPsiElement(), diagnostic.getTextRange());
    }

    @Override
    public <P> DiagnosticWithPsiElement<T> add(DiagnosticParameter<P> parameterType, P parameter) {
        map.put(parameterType, parameter);
        return this;
    }

    public <P> P getParameter(DiagnosticParameter<P> parameterType) {
        return (P) map.get(parameterType);
    }
    
    public <P> boolean hasParameter(DiagnosticParameter<P> parameterType) {
        return getParameter(parameterType) != null;
    }
}
