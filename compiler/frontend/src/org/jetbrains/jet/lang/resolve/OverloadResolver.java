package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.MutableClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.DELEGATED;

/**
 * @author Stepan Koltsov
 */
public class OverloadResolver {
    private final TopDownAnalysisContext context;

    public OverloadResolver(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    public void process() {
        checkOverloads();
    }

    private void checkOverloads() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            checkOverloadsInAClass(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            checkOverloadsInAClass(entry.getValue(), entry.getKey());
        }
    }

    private void checkOverloadsInAClass(MutableClassDescriptor classDescriptor, JetClassOrObject klass) {
        MultiMap<String, FunctionDescriptor> functionsByName = MultiMap.create();
        
        for (FunctionDescriptor function : classDescriptor.getFunctions()) {
            functionsByName.putValue(function.getName(), function);
        }
        
        for (Map.Entry<String, Collection<FunctionDescriptor>> e : functionsByName.entrySet()) {
            checkOverloadsWithSameName(e.getKey(), e.getValue(), klass);
        }

        // properties are checked elsewhere

        // Kotlin has no secondary constructors at this time

    }
    
    private void checkOverloadsWithSameName(String name, Collection<FunctionDescriptor> functions, JetClassOrObject klass) {
        if (functions.size() == 1) {
            // microoptimization
            return;
        }
        
        for (FunctionDescriptor function : functions) {
            for (FunctionDescriptor function2 : functions) {
                if (function == function2) {
                    continue;
                }

                OverloadUtil.OverloadCompatibilityInfo overloadble = OverloadUtil.isOverloadble(function, function2);
                if (!overloadble.isSuccess()) {
                    JetNamedFunction member = (JetNamedFunction) context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, function);
                    if (member == null) {
                        assert context.getTrace().get(DELEGATED, function);
                        return;
                    }

                    // XXX: mark "fun" keyword and parameters as in Java IDE // stepan.koltsov@
                    PsiElement mark = member.getNameIdentifier();
                    context.getTrace().report(Errors.CONFLICTING_OVERLOADS.on(mark, klass, function));
                }
            }
        }
    }

}
