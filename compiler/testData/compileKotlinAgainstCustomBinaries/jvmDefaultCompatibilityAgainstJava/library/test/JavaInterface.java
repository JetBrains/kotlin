package test;

public interface JavaInterface<T> {
    default T test(T p) {
        return p;
    }
}