interface Runnable {
    fun run()
}

class C {
    fun f() {
        <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class MyRunnable<!>(): Runnable {
        }
    }
}