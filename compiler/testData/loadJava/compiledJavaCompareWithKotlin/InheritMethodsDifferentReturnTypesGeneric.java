package test;

public final class InheritMethodsDifferentReturnTypesGeneric {
    public interface Super1<F, B> {
        F foo();
        B bar();
    }

    public interface Super2<FF, BB> {
        FF foo();
        BB bar();
    }

    public interface Sub extends Super1<String, CharSequence>, Super2<CharSequence, String> {
    }
}
