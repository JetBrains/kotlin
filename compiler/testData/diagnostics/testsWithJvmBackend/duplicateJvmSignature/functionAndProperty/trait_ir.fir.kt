// TARGET_BACKEND: JVM_IR

interface T {
    <!CONFLICTING_JVM_DECLARATIONS!>val x: Int<!>
        get() = 1
    <!CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
}
