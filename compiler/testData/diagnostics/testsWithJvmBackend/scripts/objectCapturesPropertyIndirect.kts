// !RENDER_DIAGNOSTICS_FULL_TEXT
// TARGET_BACKEND: JVM_IR

fun foo() = B.bar()

val life = 42

class A {
    val x = life
}

<!SCRIPT_CAPTURING_OBJECT!>object B<!> {
    fun bar() = A().x
}
