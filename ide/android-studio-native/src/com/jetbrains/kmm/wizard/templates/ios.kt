package com.jetbrains.kmm.wizard.templates

object IosMainPlatformKt : SourceTemplate("iosMain", "Platform.kt") {
    override fun render(packageName: String) = """
        package $packageName
        
        import platform.UIKit.UIDevice
        
        actual class Platform actual constructor() {
            actual val platform: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
        }
""".trimIndent()
}

object IosTestKt : SourceTemplate("iosTest", "iosTest.kt") {
    override fun render(packageName: String) = """
        package $packageName
        
        import kotlin.test.Test
        import kotlin.test.assertTrue
        
        class GreetingTest {
        
            @Test
            fun testExample() {
                assertTrue(Greeting().greeting().contains("iOS"), "Check iOS is mentioned")
            }
        }
""".trimIndent()
}