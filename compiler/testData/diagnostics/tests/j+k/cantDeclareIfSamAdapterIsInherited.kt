// FILE: Super.java
class Super {
    void foo(Runnable r) {
    }
}

// FILE: Sub.kt
class Sub1() : Super() {
    <!VIRTUAL_MEMBER_HIDDEN!>fun foo(r : (() -> Unit)?)<!> {
    }
}

class Sub2() : Super() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun foo(r : (() -> Unit)?) {
    }
}

