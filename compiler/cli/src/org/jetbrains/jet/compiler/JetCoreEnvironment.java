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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.compiler.PathUtil;

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

    @Nullable
    public static File getUnpackedRuntimePath() {
        URL url = CompileEnvironment.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("file")) {
            return new File(url.getPath()).getParentFile().getParentFile();
        }
        return null;
    }

    @Nullable
    public static File getRuntimeJarPath() {
        URL url = CompileEnvironment.class.getClassLoader().getResource("jet/JetObject.class");
        if (url != null && url.getProtocol().equals("jar")) {
            String path = url.getPath();
            return new File(path.substring(path.indexOf(":") + 1, path.indexOf("!/")));
        }
        return null;
    }

    public static File findRtJar() {
        String javaHome = System.getProperty("java.home");
        if ("jre".equals(new File(javaHome).getName())) {
            javaHome = new File(javaHome).getParent();
        }

        File rtJar = findRtJar(javaHome);

        if (rtJar == null || !rtJar.exists()) {
            throw new CompileEnvironmentException("No JDK rt.jar found under" + javaHome);
        }

        return rtJar;
    }

    private static File findRtJar(String javaHome) {
        File rtJar = new File(javaHome, "jre/lib/rt.jar");
        if (rtJar.exists()) {
            return rtJar;
        }

        File classesJar = new File(new File(javaHome).getParentFile().getAbsolutePath(), "Classes/classes.jar");
        if (classesJar.exists()) {
            return classesJar;
        }
        return null;
    }

    public void ensureRuntime() {
        Project project = getProject();
        if (JavaPsiFacade.getInstance(project).findClass("java.lang.Object", GlobalSearchScope.allScope(project)) == null) {
            // TODO: prepend
            addToClasspath(findRtJar());
        }

        if (JavaPsiFacade.getInstance(project).findClass("jet.JetObject", GlobalSearchScope.allScope(project)) == null) {
            // TODO: prepend
            File kotlin = PathUtil.getDefaultRuntimePath();
            if (kotlin == null || !kotlin.exists()) {
                kotlin = getUnpackedRuntimePath();
                if (kotlin == null) kotlin = getRuntimeJarPath();
            }
            if (kotlin == null) {
                throw new IllegalStateException("kotlin runtime not found");
            }
            addToClasspath(kotlin);
        }
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
                if(!file.isFile() || file.getPath().endsWith(".jar"))
                    addToClasspath(file);
            }
        }
    }
}
