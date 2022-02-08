// !RENDER_DIAGNOSTICS_FULL_TEXT
// TARGET_BACKEND: JVM_IR

fun foo() = B().bar()

val life = 42

<!SCRIPT_CAPTURING_INTERFACE!>interface A {
    val x get() = life
}<!>

class B : A {
    fun bar() = x
}
