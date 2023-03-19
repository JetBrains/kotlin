/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.tasks.testing.Test

fun Test.useJsIrBoxTests(
    version: Any,
    buildDir: String = "",
    fullStdLib: String = "libraries/stdlib/js-ir/build/classes/kotlin/js/main",
    reducedStdlibPath: String = "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main",
    kotlinJsTestPath: String = "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main"
) {
    setupV8()
    dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")
    dependsOn(":kotlin-test:kotlin-test-js-ir:compileKotlinJs")
    dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")

    systemProperty("kotlin.js.test.root.out.dir", buildDir)
    systemProperty("kotlin.js.full.stdlib.path", fullStdLib)
    systemProperty("kotlin.js.reduced.stdlib.path", reducedStdlibPath)
    systemProperty("kotlin.js.kotlin.test.path", kotlinJsTestPath)
    systemProperty("kotlin.js.stdlib.klib.path", "libraries/stdlib/js-ir/build/libs/kotlin-stdlib-js-ir-js-$version.klib")
}