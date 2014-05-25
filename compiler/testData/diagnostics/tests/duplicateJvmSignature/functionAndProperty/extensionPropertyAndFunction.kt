// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_PLATFORM_DECLARATIONS!>fun getX(t: Any) = 1<!>
    val Any.x: Int
        <!CONFLICTING_PLATFORM_DECLARATIONS!>get() = 1<!>
}
