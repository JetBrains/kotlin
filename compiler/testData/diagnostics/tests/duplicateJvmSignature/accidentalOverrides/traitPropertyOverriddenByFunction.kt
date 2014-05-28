trait T {
    val x: Int
        get() = 1
}

class C : T {
    <!CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
}