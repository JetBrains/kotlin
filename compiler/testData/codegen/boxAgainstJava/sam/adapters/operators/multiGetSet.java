class JavaClass {
    int get(Runnable i1, Runnable i2) {
        i1.run();
        i2.run();
        return 239;
    }

    void set(Runnable i1, Runnable i2, Runnable value) {
        i1.run();
        i2.run();
        value.run();
    }
}
