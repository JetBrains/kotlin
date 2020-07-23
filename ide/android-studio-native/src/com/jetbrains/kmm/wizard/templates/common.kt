package com.jetbrains.kmm.wizard.templates

object CommonMainPlatformKt : SourceTemplate("commonMain", "Platform.kt") {
    override fun render(packageName: String) = """
        package $packageName
    
        expect class Platform() {
            val platform: String
        }
""".trimIndent()
}

object CommonMainGreetingKt : SourceTemplate("commonMain", "Greeting.kt") {
    override fun render(packageName: String) = """
        package $packageName
    
        class Greeting {
            fun greeting(): String {
                return "Hello, ${"$"}{Platform().platform}!"
            }
        }
""".trimIndent()
}