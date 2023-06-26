// !RENDER_ALL_DIAGNOSTICS_FULL_TEXT


fun foo() = B.bar()

val life = 42

class A {
    val x = life
}

<!SCRIPT_CAPTURING_OBJECT!>object B<!> {
    fun bar() = A().x
}
