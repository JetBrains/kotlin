package test;

public interface WrongNumberOfGenericParameters {

    interface Zero {}
    interface One<T> {}

    Zero<String> z();

    One o0();
    One<String, String> o2();

    // This does not produce the expected result, because IDEA thinks Two<X> is a raw type
    interface Two<P, Q> {}
    Two<String> t1();
}