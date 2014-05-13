class JavaClass {
    interface Computable<T> {
        T compute();
    }

    static <T> T compute(Computable<T> computable) {
        return computable.compute();
    }
}
