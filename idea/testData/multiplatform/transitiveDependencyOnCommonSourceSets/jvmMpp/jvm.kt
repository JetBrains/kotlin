package test

actual class Expect {
    actual fun commonFun(): String = ""

    fun platformFun(): Int = 42
}

fun topLevelPlatformFun(): String = ""