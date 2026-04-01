// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: Test_____Result.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
class ExposedResult {
    fun consume(result: Result<String>): String = result.getOrThrow()

    fun <T: Result<String>> consume1(result: T): String = result.getOrThrow()

    fun <T: Result<String>> consumeSetValue(result: T): String {
        var local = result
        local = result
        return local.getOrThrow()
    }

    fun <T: Result<String>> consumeExpression(result: T): String {
        val local = if (true) result else result
        return local.getOrThrow()
    }
}

// FILE: Main.java
public class Main {
    public String test(kotlin.Result<String> result) {
        return new ExposedResult().consume(result);
    }
    public String test1(kotlin.Result<String> result) {
        return new ExposedResult().consume1(result);
    }
    public String testSetValue(kotlin.Result<String> result) {
        return new ExposedResult().consumeSetValue(result);
    }
    public String testExpression(kotlin.Result<String> result) {
        return new ExposedResult().consumeExpression(result);
    }
}

// FILE: Box.kt
fun box(): String  {
    var result = Main().test(Result.success("OK"))
    if (result != "OK") return result
    result = Main().test1(Result.success("OK"))
    if (result != "OK") return result
    result = Main().testSetValue(Result.success("OK"))
    if (result != "OK") return result
    return Main().testExpression(Result.success("OK"))
}
