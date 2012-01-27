package org.jetbrains.jet.plugin.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFunction;

import java.util.Collection;

/**
 * @author Nikolay Krasko
 */
public class JetShortFunctionNameIndex extends StringStubIndexExtension<JetFunction> {
    private static final JetShortClassNameIndex ourInstance = new JetShortClassNameIndex();

    public static JetShortClassNameIndex getInstance() {
        return ourInstance;
    }

    @Override
    public StubIndexKey<String, JetFunction> getKey() {
        return JetIndexKeys.TOP_LEVEL_FUNCTION_SHORT_NAME_KEY;
    }

    @Override
    public Collection<JetFunction> get(final String s, final Project project, @NotNull final GlobalSearchScope scope) {
            return super.get(s, project, new JetSourceFilterScope(scope));
    }
}
