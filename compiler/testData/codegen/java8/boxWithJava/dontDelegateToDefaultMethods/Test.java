interface Test<T> {

    T call();

    default T testDefault(T p) {
        return p;
    }
}
