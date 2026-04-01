// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
class ExposedResult {
    fun consume(result: Result<String>?): String = if (result == null) "O" else "FAIL1"

    @JvmExposeBoxed("consumeSafe")
    fun consumeRename(result: Result<String>?): String = if (result == null) "K" else "FAIL2"

    fun <T: Result<String>?> consume1(result: T): String = if (result == null) "O" else "FAIL11"

    @JvmExposeBoxed("consumeSafe1")
    fun <T: Result<String>?> consumeRename1(result: T): String = if (result == null) "K" else "FAIL21"

    fun <T: Result<String>> consume2(result: T?): String = if (result == null) "O" else "FAIL11"

    @JvmExposeBoxed("consumeSafe2")
    fun <T: Result<String>> consumeRename2(result: T?): String = if (result == null) "K" else "FAIL21"
}

// FILE: Main.java
public class Main {
    public String test() {
        return (new ExposedResult().consume(null)) + (new ExposedResult().consumeRename(null));
    }
    public String test1() {
        return (new ExposedResult().consume1(null)) + (new ExposedResult().consumeRename1(null));
    }
    public String test2() {
        return (new ExposedResult().consume2(null)) + (new ExposedResult().consumeRename2(null));
    }
}

// FILE: Box.kt
fun box(): String {
    var result = Main().test()
    if (result != "OK") return result
    result = Main().test1()
    if (result != "OK") return result
    return Main().test2()
}
