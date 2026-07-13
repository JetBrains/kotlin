// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// MODULE: lib
// FILE: lib.kt
@JvmInline
value class DepIC(val s: String)

class DepHolder(val s: DepIC) {
    fun ok(): String = s.s
}

// MODULE: main(lib)
// JVM_EXPOSE_BOXED
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
    public String usage() {
        return new UseHolder(new UseIC("OK")).ok();
    }
}

// FILE: box.kt
fun box(): String {
    val kotlin = testFromKotlin()
    if (kotlin != "OK") return kotlin

    val usage = Main().usage()
    if (usage != "OK") return "FAIL usage from Java: $usage"

    return "OK"
}
