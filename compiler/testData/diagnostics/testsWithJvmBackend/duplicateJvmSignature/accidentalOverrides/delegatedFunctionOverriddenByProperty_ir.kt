// TARGET_BACKEND: JVM_IR
interface B {
    fun getX() = 1
}

interface D {
    val x: Int
}

class C(d: D) : D by <!ACCIDENTAL_OVERRIDE!>d<!>, B