// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed("bar")
@JvmOverloads
fun foo(o: String = "O", k: String = "K"): StringWrapper = StringWrapper(o + k)

// FILE: Main.java
public class Main {
    public String test1() {
        return ICKt.bar().getS();
    }
    public String test2() {
        return ICKt.bar("O").getS();
    }
    public String test3() {
        return ICKt.bar("O", "K").getS();
    }
}

// FILE: Box.kt
fun box(): String {
    var res = Main().test1()
    if (res != "OK") return "FAIL 1: $res"
    res = Main().test2()
    if (res != "OK") return "FAIL 2: $res"
    res = Main().test3()
    if (res != "OK") return "FAIL 3: $res"
    return "OK"
}