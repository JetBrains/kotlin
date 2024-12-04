// MODULE: m1-common
// FILE: common.kt

expect class Ok(x: Int, y: String = "")

fun test() {
    Ok<!NO_VALUE_FOR_PARAMETER!>()<!>
    Ok(42)
    Ok(42, "OK")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Ok actual constructor(x: Int, y: String)

fun testJvm() {
    Ok<!NO_VALUE_FOR_PARAMETER!>()<!>
    Ok(42)
    Ok(42, "OK")
}
