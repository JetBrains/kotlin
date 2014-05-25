// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_PLATFORM_DECLARATIONS!>fun setX(x: Int) {}<!>

    var x: Int = 1
        <!CONFLICTING_PLATFORM_DECLARATIONS!>set(v) {}<!>
}