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

package org.jetbrains.jet.codegen.forTestCompile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;

public class ForTestPackJdkAnnotations {

    private ForTestPackJdkAnnotations() {
    }

    private static File jarFile = null;

    private static File getJarFile() {
        if (jarFile == null) {
            try {
                File tmpDir = JetTestUtils.tmpDir("test_jars");
                jarFile = new File(tmpDir, "jdk-annotations.jar");
                FileOutputStream annotationsJar = new FileOutputStream(jarFile);
                try {
                    JarOutputStream jarOutputStream = new JarOutputStream(new BufferedOutputStream(annotationsJar));
                    try {
                        ForTestCompileSomething.copyToJar(new File("./jdk-annotations"), jarOutputStream);
                    }
                    finally {
                        jarOutputStream.close();
                    }
                }
                finally {
                    annotationsJar.close();
                }
            }
            catch (IOException e) {
                throw new AssertionError(e);
            }
        }
        return jarFile;
    }

    @NotNull
    public static File jdkAnnotationsForTests() {
        if (ForTestCompileSomething.ACTUALLY_COMPILE) {
            return getJarFile();
        }
        File jdkAnnotations = new File("dist/kotlinc/lib/kotlin-jdk-annotations.jar");
        if (!jdkAnnotations.exists()) {
            throw new RuntimeException("Kotlin JDK annotations jar not found; please run 'ant dist' to build it");
        }
        return jdkAnnotations;
    }
}
