// TARGET_BACKEND: JVM_IR
interface T {
    fun getX() = 1
}

class C : T {
    <!ACCIDENTAL_OVERRIDE!>val x<!> = 1
}