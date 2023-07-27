// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_JVM_DECLARATIONS!>fun getX(t: Any) = 1<!>
    <!CONFLICTING_JVM_DECLARATIONS!>val Any.x: Int
        get() = 1<!>
}
