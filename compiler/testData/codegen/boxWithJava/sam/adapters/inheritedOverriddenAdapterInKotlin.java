class Super {
    public String lastCalled = null;

    void foo(Runnable r) {
        lastCalled = "super";
    }
}
