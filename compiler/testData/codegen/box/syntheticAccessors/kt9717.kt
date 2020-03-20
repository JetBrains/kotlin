// FILE: box.kt

object Test {
    val test: String = OK
}

fun box(): String = Test.test

// FILE: Vars.kt

public var OK: String = "OK"
    private set
