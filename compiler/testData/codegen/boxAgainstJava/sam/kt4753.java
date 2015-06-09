interface Base<T> {
    void call(T t);
}

interface Derived extends Base<String> {
}
