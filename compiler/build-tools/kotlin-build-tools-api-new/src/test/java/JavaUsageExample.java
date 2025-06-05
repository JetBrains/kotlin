/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.BaseCompilerArguments;
import org.jetbrains.kotlin.buildtools.api.JvmCompilationOperation;
import org.jetbrains.kotlin.buildtools.api.JvmCompilerArguments;
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain;
import org.jetbrains.kotlin.buildtools.api.KotlinToolchainKt;
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin;
import org.jetbrains.kotlin.buildtools.api.arguments.JvmTarget;
import org.jetbrains.kotlin.buildtools.api.arguments.KotlinVersion;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class JavaUsageExample {
    void basicDemo(ClassLoader classLoader) throws ExecutionException, InterruptedException {
        KotlinToolchain kotlinToolchain = KotlinToolchain.loadImplementation(classLoader);
        List<Path> kotlinSources = new ArrayList<>();
        kotlinSources.add(Paths.get("src/a.kt"));
        Path destination = Paths.get("build/classes/kotlin");
        JvmCompilationOperation compilation = kotlinToolchain.getJvm()
                .createJvmCompilationOperation(kotlinSources, destination);
        JvmCompilerArguments compilerArguments = compilation.getCompilerArguments();

        compilerArguments.set(BaseCompilerArguments.API_VERSION, KotlinVersion.KOTLIN_2_1);
        compilerArguments.set(BaseCompilerArguments.LANGUAGE_VERSION, KotlinVersion.KOTLIN_2_2);
        compilerArguments.set(BaseCompilerArguments.PROGRESSIVE, true);
        compilerArguments.set(BaseCompilerArguments.OPT_IN, Collections.singletonList("my.custom.OptInAnnotation"));

        CompilerPlugin allOpenPlugin = new CompilerPlugin(
                Paths.get("../plugin-jar"),
                "org.jetbrains.kotlin.allopen",
                Map.of("preset", "spring")
        );
        List<CompilerPlugin> plugins = new ArrayList<>();
        plugins.add(allOpenPlugin);

        compilerArguments.set(BaseCompilerArguments.COMPILER_PLUGINS, plugins);

        compilerArguments.set(JvmCompilerArguments.JVM_TARGET, JvmTarget.JVM_21);

        KotlinToolchainKt.executeOperation(kotlinToolchain, compilation).get();
    }
}
