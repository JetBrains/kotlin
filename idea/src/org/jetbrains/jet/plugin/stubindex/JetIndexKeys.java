package org.jetbrains.jet.plugin.stubindex;

import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFunction;

/**
 * @author Nikolay Krasko
 */
public interface JetIndexKeys {
    StubIndexKey<String, JetClassOrObject> SHORT_NAME_KEY = StubIndexKey.createIndexKey("jet.class.shortName");
    StubIndexKey<String, JetClassOrObject> FQN_KEY = StubIndexKey.createIndexKey("jet.fqn");
    
    StubIndexKey<String, JetFunction> TOP_LEVEL_FUNCTION_SHORT_NAME_KEY = StubIndexKey.createIndexKey("jet.top.level.function.short.name");
//    StubIndexKey<String, JetFunction> TOP_LEVEL_FUNCTION_FQN_KEY = StubIndexKey.createIndexKey("jet.top.level.function.short.name");

//    StubIndexKey<String, JetObjectDeclaration> PACKAGE_OBJECT_KEY = StubIndexKey.createIndexKey("jet.package.object.fqn");
//    StubIndexKey<String, JetFunction> EXTENSION_FUNCTION_TO_TYPE_FQN = StubIndexKey.createIndexKey("jet.extension.function.to.type.fqn");
}

