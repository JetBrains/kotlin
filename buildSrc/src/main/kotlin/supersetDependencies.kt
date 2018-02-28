import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */



fun DependencyHandlerScope.intellijKompotDep(p: Project) {
    val compileOnly by p.configurations
    compileOnly(p.files("${p.rootDir}/../../TestCompat/build/IC-superset/libClasses"))
    compileOnly("org.jetbrains.kompot:source-api:0.0.1") {
        isTransitive = false
    }
}

fun DependencyHandlerScope.intellijPluginKompotDep(p: Project, name: String) {
    val compileOnly by p.configurations
    compileOnly(p.files("${p.rootDir}/../../TestCompat/build/IC-superset/plugins/$name/libClasses"))
}