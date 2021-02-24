// TARGET_BACKEND: JVM_IR
interface T {
    val x: Int
        get() = 1
}

class C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}