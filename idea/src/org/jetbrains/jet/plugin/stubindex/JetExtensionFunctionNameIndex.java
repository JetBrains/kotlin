package org.jetbrains.jet.plugin.stubindex;

import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.jet.lang.psi.JetNamedFunction;

/**
 * @author Nikolay Krasko
 */
public class JetExtensionFunctionNameIndex extends StringStubIndexExtension<JetNamedFunction> {
    private static final JetExtensionFunctionNameIndex instance = new JetExtensionFunctionNameIndex();

    public static JetExtensionFunctionNameIndex getInstance() {
        return instance;
    }

    @Override
    public StubIndexKey<String, JetNamedFunction> getKey() {
        return JetIndexKeys.EXTENSION_FUNCTION_SHORT_NAME_KEY;
    }
}
