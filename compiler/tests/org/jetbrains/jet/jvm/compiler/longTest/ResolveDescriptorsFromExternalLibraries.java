/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.jvm.compiler.longTest;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.TimeUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolverUtil;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.utils.PathUtil;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public class ResolveDescriptorsFromExternalLibraries {


    public static void main(String[] args) throws Exception {
        boolean hasErrors = new ResolveDescriptorsFromExternalLibraries().run();
        System.out.println("$");
        if (hasErrors) {
            System.exit(1);
        }
    }

    private static List<File> findJarsInDirectory(@NotNull File directory) {
        List<File> r = Lists.newArrayList();

        List<File> stack = Lists.newArrayList();
        stack.add(directory);
        while (!stack.isEmpty()) {
            File file = stack.get(stack.size() - 1);
            stack.remove(stack.size() - 1);
            if (file.isFile() && file.getName().endsWith(".jar")) {
                r.add(file);
            }
            File[] children = file.listFiles();
            if (children != null) {
                stack.addAll(Arrays.asList(children));
            }
        }
        return r;
    }

    private boolean run() throws Exception {
        boolean hasErrors = false;

        hasErrors |= testLibrary("org.scala-lang", "scala-library", "2.9.2", 1);

        hasErrors |= testLibraryFile(null, "rt.jar", 1000);

        for (File jar : findJarsInDirectory(new File("ideaSDK"))) {
            hasErrors |= testLibraryFile(jar, jar.getPath(), 1000);
        }

        hasErrors |= testLibrary("com.google.guava", "guava", "12.0-rc2", 1000);
        hasErrors |= testLibrary("org.springframework", "spring-core", "3.1.1.RELEASE", 1000);
        hasErrors |= testLibrary("com.vaadin", "vaadin", "6.6.8", 1000);
        hasErrors |= testLibraryFromUrl("http://mcvaadin.googlecode.com/files/mcvaadin.jar", 1000);
        return hasErrors;
    }

    private boolean testLibraryFromUrl(@NotNull String urlJar, int classesPerChunk) throws Exception {
        return testLibraryFile(getLibraryFromUrl(urlJar), urlJar, classesPerChunk);
    }

    private boolean testLibrary(@NotNull String org, @NotNull String module, @NotNull String rev, int classesPerChunk) throws Exception {
        LibFromMaven lib = new LibFromMaven(org, module, rev);
        File jar = getLibraryFromMaven(lib);

        return testLibraryFile(jar, lib.toString(), classesPerChunk);
    }

    private boolean testLibraryFile(@Nullable File jar, @NotNull String libDescription, int classesPerChunk) throws IOException {
        System.out.println("Testing library " + libDescription + "...");
        if (jar != null) {
            System.out.println("Using file " + jar);
        }
        else {
            jar = PathUtil.findRtJar();
            System.out.println("Using rt.jar: " + jar);
        }

        long start = System.currentTimeMillis();

        FileInputStream is = new FileInputStream(jar);
        boolean hasErrors = false;
        try {
            ZipInputStream zip = new ZipInputStream(is);

            while (zip.available() > 0) {
                hasErrors |= parseLibraryFileChunk(jar, libDescription, zip, classesPerChunk);
            }
        } finally {
            try {
                is.close();
            } catch (Throwable e) {}
        }

        System.out.println("Testing library " + libDescription + " done in " + TimeUtils.millisecondsToSecondsString(System.currentTimeMillis() - start) + "s " + (hasErrors ? "with" : "without") + " errors");

        return hasErrors;
    }

    private boolean parseLibraryFileChunk(File jar, String libDescription, ZipInputStream zip, int classesPerChunk) throws IOException {
        Disposable junk = new Disposable() {
            @Override
            public void dispose() { }
        };

        JetCoreEnvironment jetCoreEnvironment;
        if (jar != null) {
            jetCoreEnvironment = JetCoreEnvironment.createForTests(junk, JetTestUtils.compilerConfigurationForTests(
                    ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), jar));
        }
        else {
            CompilerConfiguration configuration =
                    JetTestUtils.compilerConfigurationForTests(ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK);
            jetCoreEnvironment = JetCoreEnvironment.createForTests(junk, configuration);
            if (!PathUtil.findRtJar().equals(jar)) {
                throw new RuntimeException("rt.jar mismatch: " + jar + ", " + PathUtil.findRtJar());
            }
        }

        InjectorForJavaDescriptorResolver injector = InjectorForJavaDescriptorResolverUtil.create(
                jetCoreEnvironment.getProject(), new BindingTraceContext());
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

        boolean hasErrors;
        try {

            hasErrors = false;

            for (int count = 0; count < classesPerChunk; ) {
                ZipEntry entry = zip.getNextEntry();
                if (entry == null) {
                    break;
                }

                if (count == 0) {
                    System.err.println("chunk from " + entry.getName());
                }

                System.err.println(entry.getName());

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

                try {
                    ClassDescriptor clazz = javaDescriptorResolver.resolveClass(new FqName(className), IGNORE_KOTLIN_SOURCES);
                    if (clazz == null) {
                        throw new IllegalStateException("class not found by name " + className + " in " + libDescription);
                    }
                    clazz.getDefaultType().getMemberScope().getAllDescriptors();
                }
                catch (Exception e) {
                    System.err.println("failed to resolve " + className);
                    e.printStackTrace();
                    //throw new RuntimeException("failed to resolve " + className + ": " + e, e);
                    hasErrors = true;
                }

                ++count;
            }
        }
        finally {
            Disposer.dispose(junk);
        }
        return hasErrors;
    }

    @NotNull
    private File getLibraryFromUrl(@NotNull String url) throws Exception {

        String fileName = url
                .replaceAll("^http://", "")
                .replaceAll("/", "_")
                ;
        File dir = new File(userHome(), ".kotlin-project/resolve-libraries");

        File file = new File(dir, fileName);

        return getFileFromUrl(url, file, url);
    }

    @NotNull
    private File getLibraryFromMaven(@NotNull LibFromMaven lib) throws Exception {
        String fileName = lib.getModule() + "-" + lib.getRev() + ".jar";

        File dir = new File(userHome(), ".kotlin-project/resolve-libraries/" + lib.getOrg() + "/" + lib.getModule());

        File file = new File(dir, fileName);
        String uri = url(lib);

        return getFileFromUrl(lib.toString(), file, uri);
    }

    private File getFileFromUrl(@NotNull String lib, @NotNull File file, @NotNull String uri) throws IOException {
        if (file.exists()) {
            return file;
        }

        JetTestUtils.mkdirs(file.getParentFile());

        File tmp = new File(file.getPath() + "~");

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

    private static File userHome() {return new File(System.getProperty("user.home"));}

    private static String url(LibFromMaven lib) {
        return "http://repo1.maven.org/maven2/" + lib.getOrg().replace(".", "/") + "/" + lib.getModule() + "/" + lib.getRev()
                + "/" + lib.getModule() + "-" + lib.getRev() + ".jar";
    }
}
