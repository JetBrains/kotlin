/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ant;

import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.Javac13;
import org.apache.tools.ant.types.Path;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class KotlinCompilerAdapter extends Javac13 {
    private static final List<String> KOTLIN_EXTENSIONS = Arrays.asList("kt", "kts");

    private Path externalAnnotations;

    public void setExternalAnnotations(Path externalAnnotations) {
        this.externalAnnotations = externalAnnotations;
    }

    public Path createExternalAnnotations() {
        if (externalAnnotations == null) {
            externalAnnotations = new Path(getProject());
        }
        return externalAnnotations.createPath();
    }

    @Override
    public String[] getSupportedFileExtensions() {
        List<String> result = KotlinPackage.plus(Arrays.asList(super.getSupportedFileExtensions()), KOTLIN_EXTENSIONS);
        //noinspection SSBasedInspection
        return result.toArray(new String[result.size()]);
    }

    @Override
    public boolean execute() throws BuildException {
        Javac javac = getJavac();

        Kotlin2JvmTask kotlinc = new Kotlin2JvmTask();
        kotlinc.setOutput(javac.getDestdir());

        Path classpath = javac.getClasspath();
        if (classpath != null) {
            kotlinc.setClasspath(classpath);
        }

        // We use the provided src dir instead of compileList, because the latter is insane:
        // it is constructed only of sources which are newer than classes with the same name
        kotlinc.setSrc(javac.getSrcdir());

        kotlinc.setExternalAnnotations(externalAnnotations);

        kotlinc.execute();

        javac.log("Running javac...");

        // Javac13#execute passes everything in compileList to javac, which doesn't recognize .kt files
        compileList = filterOutKotlinSources(compileList);

        return compileList.length == 0 || super.execute();
    }

    @NotNull
    private static File[] filterOutKotlinSources(@NotNull File[] files) {
        List<File> nonKotlinSources = KotlinPackage.filterNot(files, new Function1<File, Boolean>() {
            @Override
            public Boolean invoke(File file) {
                for (String extension : KOTLIN_EXTENSIONS) {
                    if (file.getPath().endsWith("." + extension)) return true;
                }
                return false;
            }
        });

        return nonKotlinSources.toArray(new File[nonKotlinSources.size()]);
    }
}
