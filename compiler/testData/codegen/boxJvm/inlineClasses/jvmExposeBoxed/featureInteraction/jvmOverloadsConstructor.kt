// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// With '@JvmOverloads' every arity gets a boxed constructor.
class WithOverloads @JvmOverloads @JvmExposeBoxed constructor(val first: Id = Id("O"), val second: Id = Id("K")) {
    fun joined(): String = first.value + second.value
}

// Without it, only the full parameter list is exposed: no boxed '$default' constructor may appear in the
// listing, while the mangled '$default' one stays for Kotlin callers.
class WithoutOverloads @JvmExposeBoxed constructor(val first: Id = Id("O"), val second: Id = Id("K")) {
    fun joined(): String = first.value + second.value
}

// FILE: Main.java
public class Main {
    public String noArguments() {
        return new WithOverloads().joined();
    }

    public String allArguments() {
        return new WithoutOverloads(new Id("O"), new Id("K")).joined();
    }
}

// FILE: Box.kt
fun box(): String {
    var res = Main().noArguments()
    if (res != "OK") return "FAIL 1: $res"
    res = Main().allArguments()
    if (res != "OK") return "FAIL 2: $res"
    res = WithoutOverloads().joined()
    if (res != "OK") return "FAIL 3: $res"
    return "OK"
}
