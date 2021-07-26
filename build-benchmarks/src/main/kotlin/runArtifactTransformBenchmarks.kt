/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun main() {
    mainImpl(kotlinBenchmarks(arrayOf("-Dkotlin.incremental.useClasspathSnapshot=true")), "../.") // expected working dir is %KOTLIN_PROJECT_PATH%/build-benchmarks/
}