// !RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// KT-19423 variation

val used = "abc"

class Outer {
    val middle = used
    <!SCRIPT_CAPTURING_NESTED_CLASS!>class User {
        val property = used
    }<!>
}
