package org.jetbrains.jet.plugin.stubindex;

import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetNamedFunction;

/**
 * @author Nikolay Krasko
 */
public interface JetIndexKeys {
    StubIndexKey<String, JetClassOrObject> SHORT_NAME_KEY = StubIndexKey.createIndexKey("jet.class.shortName");
    StubIndexKey<String, JetClassOrObject> FQN_KEY = StubIndexKey.createIndexKey("jet.fqn");

    StubIndexKey<String, JetNamedFunction> TOP_LEVEL_FUNCTION_SHORT_NAME_KEY = StubIndexKey.createIndexKey("jet.top.level.function.short.name");
    StubIndexKey<String, JetNamedFunction> TOP_LEVEL_FUNCTION_FQNAME_KEY = StubIndexKey.createIndexKey("jet.top.level.function.fqname");

    StubIndexKey<String, JetNamedFunction> EXTENSION_FUNCTION_SHORT_NAME_KEY = StubIndexKey.createIndexKey("jet.top.level.extension.function.short.name");
    StubIndexKey<String, JetNamedFunction> EXTENSION_FUNCTION_FQNAME_KEY = StubIndexKey.createIndexKey("jet.top.level.extension.function.fqname");
}

