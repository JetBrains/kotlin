package org.jetbrains.jet.plugin.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetNamedFunction;

import java.util.Collection;

/**
 * @author Nikolay Krasko
 */
public class JetShortFunctionNameIndex extends StringStubIndexExtension<JetNamedFunction> {
    private static final JetShortFunctionNameIndex ourInstance = new JetShortFunctionNameIndex();

    public static JetShortFunctionNameIndex getInstance() {
        return ourInstance;
    }

    @Override
    public StubIndexKey<String, JetNamedFunction> getKey() {
        return JetIndexKeys.TOP_LEVEL_FUNCTION_SHORT_NAME_KEY;
    }

    @Override
    public Collection<JetNamedFunction> get(final String s, final Project project, @NotNull final GlobalSearchScope scope) {
        return super.get(s, project, new JetSourceFilterScope(scope));
    }
}
