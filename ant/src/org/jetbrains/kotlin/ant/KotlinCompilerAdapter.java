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
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.Javac13;
import org.apache.tools.ant.taskdefs.condition.AntVersion;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.tools.ant.Project.MSG_WARN;

public class KotlinCompilerAdapter extends Javac13 {
    private static final List<String> KOTLIN_EXTENSIONS = Arrays.asList("kt", "kts");

    private Path externalAnnotations;

    private String moduleName;

    public List<Commandline.Argument> additionalArguments = new ArrayList<Commandline.Argument>(0);

    public void setExternalAnnotations(Path externalAnnotations) {
        this.externalAnnotations = externalAnnotations;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Path createExternalAnnotations() {
        if (externalAnnotations == null) {
            externalAnnotations = new Path(getProject());
        }
        return externalAnnotations.createPath();
    }

    public Commandline.Argument createCompilerArg() {
        Commandline.Argument argument = new Commandline.Argument();
        additionalArguments.add(argument);
        return argument;
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

        checkAntVersion();

        Kotlin2JvmTask kotlinc = new Kotlin2JvmTask();
        kotlinc.setFailOnError(javac.getFailonerror());
        kotlinc.setOutput(javac.getDestdir());

        Path classpath = javac.getClasspath();
        if (classpath != null) {
            kotlinc.setClasspath(classpath);
        }

        // We use the provided src dir instead of compileList, because the latter is insane:
        // it is constructed only of sources which are newer than classes with the same name
        kotlinc.setSrc(javac.getSrcdir());

        kotlinc.setExternalAnnotations(externalAnnotations);

        if (moduleName == null) {
            moduleName = AntPackage.getDefaultModuleName(javac);
        }
        kotlinc.setModuleName(moduleName);

        kotlinc.getAdditionalArguments().addAll(additionalArguments);

        // Javac13#execute passes everything in compileList to javac, which doesn't recognize .kt files
        File[] compileListForJavac = filterOutKotlinSources(compileList);

        boolean hasKotlinFilesInSources = compileListForJavac.length < compileList.length;

        if (hasKotlinFilesInSources) {
            kotlinc.execute();
            if (!Integer.valueOf(0).equals(kotlinc.getExitCode())) {
                // Don't run javac if failOnError = false and there were errors on Kotlin sources
                return false;
            }
        }
        else {
            // This is needed for addRuntimeToJavacClasspath, where kotlinc arguments will be used.
            kotlinc.fillArguments();
        }

        javac.log("Running javac...");

        compileList = compileListForJavac;

        addRuntimeToJavacClasspath(kotlinc);

        return compileList.length == 0 || super.execute();
    }

    private void addRuntimeToJavacClasspath(@NotNull Kotlin2JvmTask kotlinc) {
        for (String arg : kotlinc.getArgs()) {
            // If "-no-stdlib" was specified explicitly, probably the user also wanted the javac classpath to not have it
            if ("-no-stdlib".equals(arg)) return;
        }

        if (compileClasspath == null) {
            compileClasspath = new Path(getProject());
        }
        compileClasspath.add(new Path(getProject(), KotlinAntTaskUtil.INSTANCE$.getRuntimeJar().getAbsolutePath()));
    }

    private void checkAntVersion() {
        AntVersion checkVersion = new AntVersion();
        checkVersion.setAtLeast("1.8.2");
        if (!checkVersion.eval()) {
            getJavac().log("<withKotlin> task requires Ant of version at least 1.8.2 to operate reliably. " +
                           "Please upgrade or, as a workaround, make sure you have at least one Java source and " +
                           "the output directory is clean before running this task. " +
                           "You have: " + getProject().getProperty(MagicNames.ANT_VERSION), MSG_WARN);
        }
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
