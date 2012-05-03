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

package org.jetbrains.jet.cli.jvm.compiler.longTest;

import com.google.common.io.ByteStreams;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TimeUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Stepan Koltsov
 */
public class ResolveDescriptorsFromExternalLibraries {


    public static void main(String[] args) throws Exception {
        new ResolveDescriptorsFromExternalLibraries().run();
        System.out.println("$");
    }

    private void run() throws Exception {
        testLibraryFile(null, "rt.jar");
        testLibrary("com.google.guava", "guava", "12.0-rc2");
        testLibrary("org.springframework", "spring-core", "3.1.1.RELEASE");
    }

    private void testLibrary(@NotNull String org, @NotNull String module, @NotNull String rev) throws Exception {
        LibFromMaven lib = new LibFromMaven(org, module, rev);
        File jar = getLibrary(lib);

        testLibraryFile(jar, lib.toString());
    }

    private void testLibraryFile(@Nullable File jar, @NotNull String libDescription) throws IOException {
        System.out.println("Testing library " + libDescription + "...");
        if (jar != null) {
            System.out.println("Using file " + jar);
        }
        else {
            System.out.println("Using rt.jar");
        }

        long start = System.currentTimeMillis();

        Disposable junk = new Disposable() {
            @Override
            public void dispose() { }
        };

        JetCoreEnvironment jetCoreEnvironment;
        if (jar != null) {
            jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(junk, CompilerSpecialMode.STDLIB);
            jetCoreEnvironment.addToClasspath(jar);
        }
        else {
            CompilerDependencies compilerDependencies = CompileCompilerDependenciesTest.compilerDependenciesForTests(CompilerSpecialMode.STDLIB, false);
            jetCoreEnvironment = JetCoreEnvironment.getCoreEnvironmentForJVM(junk, compilerDependencies);
            jar = compilerDependencies.getJdkJar();
        }


        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(jetCoreEnvironment.getCompilerDependencies(), jetCoreEnvironment.getProject());

        FileInputStream is = new FileInputStream(jar);
        try {
            ZipInputStream zip = new ZipInputStream(is);
            for (;;) {
                ZipEntry entry = zip.getNextEntry();
                if (entry == null) {
                    break;
                }
                String entryName = entry.getName();
                if (!entryName.endsWith(".class")) {
                    continue;
                }
                if (entryName.matches("(.*/|)package-info\\.class")) {
                    continue;
                }
                if (entryName.contains("$")) {
                    continue;
                }
                String className = entryName.substring(0, entryName.length() - ".class".length()).replace("/", ".");
                ClassDescriptor clazz = injector.getJavaDescriptorResolver().resolveClass(new FqName(className), DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
                if (clazz == null) {
                    throw new IllegalStateException("class not found by name " + className + " in " + libDescription);
                }
                clazz.getDefaultType().getMemberScope().getAllDescriptors();
            }

        } finally {
            try {
                is.close();
            }
            catch (Throwable e) {}

            Disposer.dispose(junk);
        }

        System.out.println("Testing library " + libDescription + " done in " + TimeUtils.millisecondsToSecondsString(System.currentTimeMillis() - start) + "s");
    }

    @NotNull
    private File getLibrary(@NotNull LibFromMaven lib) throws Exception {
        File userHome = new File(System.getProperty("user.home"));
        String fileName = lib.getModule() + "-" + lib.getRev() + ".jar";

        File dir = new File(userHome, ".kotlin-project/resolve-libraries/" + lib.getOrg() + "/" + lib.getModule());

        File file = new File(dir, fileName);
        if (file.exists()) {
            return file;
        }

        JetTestUtils.mkdirs(dir);

        File tmp = new File(dir, fileName + "~");

        String uri = url(lib);
        GetMethod method = new GetMethod(uri);

        FileOutputStream os = null;

        System.out.println("Downloading library " + lib + " to " + file);

        MultiThreadedHttpConnectionManager connectionManager = null;
        try {
            connectionManager = new MultiThreadedHttpConnectionManager();
            HttpClient httpClient = new HttpClient(connectionManager);
            os = new FileOutputStream(tmp);
            String userAgent = ResolveDescriptorsFromExternalLibraries.class.getName() + "/commons-httpclient";
            method.getParams().setParameter(HttpMethodParams.USER_AGENT, userAgent);
            int code = httpClient.executeMethod(method);
            if (code != 200) {
                throw new RuntimeException("failed to execute GET " + uri + ", code is " + code);
            }
            InputStream responseBodyAsStream = method.getResponseBodyAsStream();
            if (responseBodyAsStream == null) {
                throw new RuntimeException("method is executed fine, but response is null");
            }
            ByteStreams.copy(responseBodyAsStream, os);
            os.close();
            if (!tmp.renameTo(file)) {
                throw new RuntimeException("failed to rename file: " + tmp + " to " + file);
            }
            return file;
        }
        finally {
            if (method != null) {
                try {
                    method.releaseConnection();
                }
                catch (Throwable e) {}
            }
            if (connectionManager != null) {
                try {
                connectionManager.shutdown();
                }
                catch (Throwable e) {}
            }
            if (os != null) {
                try {
                    os.close();
                }
                catch (Throwable e) {}
            }
            tmp.delete();
        }
    }

    private static String url(LibFromMaven lib) {
        return "http://repo1.maven.org/maven2/" + lib.getOrg().replace(".", "/") + "/" + lib.getModule() + "/" + lib.getRev()
                + "/" + lib.getModule() + "-" + lib.getRev() + ".jar";
    }
}
