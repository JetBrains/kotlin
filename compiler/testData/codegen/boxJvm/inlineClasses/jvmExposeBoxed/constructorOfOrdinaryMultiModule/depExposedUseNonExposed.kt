// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// MODULE: lib
// FILE: lib.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class DepIC @JvmExposeBoxed constructor(val s: String)

class DepHolder @JvmExposeBoxed constructor(val s: DepIC) {
    fun ok(): String = s.s
}

// MODULE: main(lib)
// FILE: usage.kt
@JvmInline
value class UseIC(val s: String)

class UseHolder(val s: UseIC) {
    fun ok(): String = s.s
}

fun testFromKotlin(): String {
    val dependency = DepHolder(DepIC("OK")).ok()
    if (dependency != "OK") return "FAIL dependency from Kotlin: $dependency"

    val usage = UseHolder(UseIC("OK")).ok()
    if (usage != "OK") return "FAIL usage from Kotlin: $usage"

    return "OK"
}

// FILE: Main.java
public class Main {
    public String dependency() {
        return new DepHolder(new DepIC("OK")).ok();
    }
}

// FILE: box.kt
fun box(): String {
    val kotlin = testFromKotlin()
    if (kotlin != "OK") return kotlin

    val dependency = Main().dependency()
    if (dependency != "OK") return "FAIL dependency from Java: $dependency"

    return "OK"
}
