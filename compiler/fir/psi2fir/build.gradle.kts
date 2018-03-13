/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    val compile by configurations
    val compileOnly by configurations

    compile(project(":compiler:psi"))
    compile(project(":core:descriptors"))
    compile(project(":compiler:fir:tree"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "annotations") }
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}