class Super {
    public String lastCalled = null;

    void foo(Runnable r) {
        lastCalled = "super";
    }
}

class Sub extends Super {
    void foo(kotlin.jvm.functions.Function0<kotlin.Unit> r) {
        lastCalled = "sub";
    }
}
