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
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementFinder;
import org.jetbrains.jet.asJava.JavaElementFinder;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class JetCoreEnvironment extends JavaCoreEnvironment {
    private List<CompilerPlugin> compilerPlugins = new ArrayList<CompilerPlugin>();
    private CompileSession session;

    public JetCoreEnvironment(Disposable parentDisposable, boolean includeJdkHeaders) {
        super(parentDisposable);
        registerFileType(JetFileType.INSTANCE, "kt");
        registerFileType(JetFileType.INSTANCE, "kts");
        registerFileType(JetFileType.INSTANCE, "ktm");
        registerFileType(JetFileType.INSTANCE, "jet");
        registerParserDefinition(new JavaParserDefinition());
        registerParserDefinition(new JetParserDefinition());


        myProject.registerService(JetFilesProvider.class, new CliJetFilesProvider(this));
        Extensions.getArea(myProject)
                .getExtensionPoint(PsiElementFinder.EP_NAME)
                .registerExtension(new JavaElementFinder(myProject));

        if (includeJdkHeaders) {
            for (VirtualFile root : PathUtil.getAltHeadersRoots()) {
                addLibraryRoot(root);
            }
        }

        JetStandardLibrary.initialize(getProject());
    }

    public MockApplication getApplication() {
        return myApplication;
    }

    public List<CompilerPlugin> getCompilerPlugins() {
        return compilerPlugins;
    }

    public void setCompilerPlugins(List<CompilerPlugin> compilerPlugins) {
        this.compilerPlugins = compilerPlugins;
    }

    public void addToClasspathFromClassLoader(ClassLoader loader) {
        ClassLoader parent = loader.getParent();
        if(parent != null)
            addToClasspathFromClassLoader(parent);
        
        if(loader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) loader).getURLs()) {
                File file = new File(url.getPath());
                if(file.exists() && (!file.isFile() || file.getPath().endsWith(".jar")))
                    addToClasspath(file);
            }
        }
    }

    public void setSession(CompileSession session) {
        this.session = session;
    }

    public CompileSession getSession() {
        return session;
    }
}
