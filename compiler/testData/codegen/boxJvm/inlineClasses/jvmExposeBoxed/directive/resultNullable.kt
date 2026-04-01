// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ImplicitJvmExposeBoxed
// CHECK_BYTECODE_LISTING

// FILE: Test.kt
class ExposedResult {
    fun consume(result: Result<String>?): String = if (result == null) "OK" else "FAIL0"

    fun <T: Result<String>> consume1(result: T?): String = if (result == null) "OK" else "FAIL1"

    fun <T: Result<String>?> consume2(result: T): String = if (result == null) "OK" else "FAIL2"
}

// FILE: Main.java
public class Main {
    public String test() {
        return new ExposedResult().consume(null);
    }
    public String test1() {
        return new ExposedResult().consume1(null);
    }
    public String test2() {
        return new ExposedResult().consume2(null);
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
