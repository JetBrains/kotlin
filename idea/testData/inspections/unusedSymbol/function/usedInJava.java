class RandomJavaClass {
    void g() {
        foo.UsedInJavaKt.usedInJava();
    }

    public void context() {
        foo.Ctor c = new foo.Ctor(0);
        c.justCompare();
    }
}