// TARGET_BACKEND: JVM_IR

interface T {
    val x: Int
        get() = 1
}

interface C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}
