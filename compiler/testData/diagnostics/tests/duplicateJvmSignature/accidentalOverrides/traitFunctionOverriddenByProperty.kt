trait T {
    fun getX() = 1
}

class C : T {
    val x: Int
        <!CONFLICTING_PLATFORM_DECLARATIONS!>get() = 1<!>
}