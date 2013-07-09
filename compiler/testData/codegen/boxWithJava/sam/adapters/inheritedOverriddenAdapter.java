class Super {
    public String lastCalled = null;

    void foo(Runnable r) {
        lastCalled = "super";
    }
}

class Sub extends Super {
    void foo(jet.Function0<jet.Unit> r) {
        lastCalled = "sub";
    }
}
