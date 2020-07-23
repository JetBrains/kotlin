/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard.templates

object AndroidMainPlatformKt : SourceTemplate("androidMain", "Platform.kt") {
    override fun render(packageName: String) = """
        package $packageName

        actual class Platform actual constructor() {
            actual val platform: String = "Android ${"$"}{android.os.Build.VERSION.SDK_INT}"
        }
""".trimIndent()
}

object AndroidTestKt : SourceTemplate("androidTest", "androidTest.kt") {
    override fun render(packageName: String) = """
        package $packageName
        
        import org.junit.Assert.assertTrue
        import org.junit.Test
        
        class GreetingTest {
        
            @Test
            fun testExample() {
                assertTrue("Check Android is mentioned", Greeting().greeting().contains("Android"))
            }
        }
""".trimIndent()
}

fun androidManifestXml(entryPointsPackage: String): String {
    return """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$entryPointsPackage"/>
""".trimIndent()
}