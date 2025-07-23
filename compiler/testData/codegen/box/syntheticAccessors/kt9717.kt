// JVM_ABI_K1_K2_DIFF: KT-63984

// FILE: Vars.kt

public var OK: String = "OK"
    private set

// FILE: box.kt

object Test {
    val test: String = OK
}

fun box(): String = Test.test
