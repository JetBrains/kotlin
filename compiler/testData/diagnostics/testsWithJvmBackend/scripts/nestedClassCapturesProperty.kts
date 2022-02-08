// !RENDER_DIAGNOSTICS_FULL_TEXT
// TARGET_BACKEND: JVM_IR

// KT-19423 variation
val used = "abc"

class Outer {
    <!SCRIPT_CAPTURING_NESTED_CLASS!>class User {
        val property = used
    }<!>
}
