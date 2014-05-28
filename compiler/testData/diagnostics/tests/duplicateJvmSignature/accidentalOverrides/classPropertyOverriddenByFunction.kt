open class B {
    val x: Int
        get() = 1
}

class C : B() {
    <!CONFLICTING_PLATFORM_DECLARATIONS!>fun getX()<!> = 1
}