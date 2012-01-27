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
public class JetFullClassNameIndex extends StringStubIndexExtension<JetClassOrObject> {

    private static final JetFullClassNameIndex ourInstance = new JetFullClassNameIndex();
    public static JetFullClassNameIndex getInstance() {
        return ourInstance;
    }

    @Override
    public StubIndexKey<String, JetClassOrObject> getKey() {
        return JetIndexKeys.FQN_KEY;
    }

    @Override
    public Collection<JetClassOrObject> get(final String integer, final Project project, @NotNull final GlobalSearchScope scope) {
        return super.get(integer, project, new JetSourceFilterScope(scope));
    }
}
