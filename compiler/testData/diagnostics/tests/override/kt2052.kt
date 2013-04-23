trait Runnable {
    fun run()
}

class C {
    fun f() {
        class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MyRunnable<!>(): Runnable {
        }
    }
}