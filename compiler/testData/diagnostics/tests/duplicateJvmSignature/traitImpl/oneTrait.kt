trait T {
    fun getX() = 1
}

class C : T {
    <!CONFLICTING_PLATFORM_DECLARATIONS!>val x<!> = 1
}