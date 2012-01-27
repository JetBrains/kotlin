package org.jetbrains.jet.plugin.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClassOrObject;

import java.util.Collection;

/**
 * @author Nikolay Krasko
 */
public class JetShortClassNameIndex extends StringStubIndexExtension<JetClassOrObject> {
    private static final JetShortClassNameIndex ourInstance = new JetShortClassNameIndex();
    public static JetShortClassNameIndex getInstance() {
        return ourInstance;
    }

    @Override
    public StubIndexKey<String, JetClassOrObject> getKey() {
        return JetIndexKeys.SHORT_NAME_KEY;
    }

    @Override
    public Collection<JetClassOrObject> get(final String s, final Project project, @NotNull final GlobalSearchScope scope) {
        return super.get(s, project, new JetSourceFilterScope(scope));
    }
}
