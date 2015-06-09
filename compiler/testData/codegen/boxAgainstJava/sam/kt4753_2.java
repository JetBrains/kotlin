class ParamBase<T> {}

interface F<T> extends FBase<ParamBase<? extends T>> {
}

interface FBase<T> {
    void call(T t);
}
