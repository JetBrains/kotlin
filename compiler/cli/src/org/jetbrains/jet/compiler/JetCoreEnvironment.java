/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.compiler;

import com.intellij.core.JavaCoreEnvironment;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.compiler.PathUtil;

/**
 * @author yole
 */
public class JetCoreEnvironment extends JavaCoreEnvironment {
    public JetCoreEnvironment(Disposable parentDisposable) {
        super(parentDisposable);
        registerFileType(JetFileType.INSTANCE, "kt");
        registerFileType(JetFileType.INSTANCE, "kts");
        registerFileType(JetFileType.INSTANCE, "ktm");
        registerFileType(JetFileType.INSTANCE, "jet");
        registerParserDefinition(new JavaParserDefinition());
        registerParserDefinition(new JetParserDefinition());

/*
        Extensions.getArea(myProject)
                .getExtensionPoint(PsiElementFinder.EP_NAME)
                .registerExtension(new JavaElementFinder(myProject));
*/

        for (VirtualFile root : PathUtil.getAltHeadersRoots()) {
            addLibraryRoot(root);
        }

        JetStandardLibrary.initialize(getProject());
    }

    public MockApplication getApplication() {
        return myApplication;
    }
}
