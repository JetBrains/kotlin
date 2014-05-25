trait T {
    val x: Int
        get() = 1
}

class C : T {
    <!CONFLICTING_PLATFORM_DECLARATIONS!>fun getX() = 1<!>
}