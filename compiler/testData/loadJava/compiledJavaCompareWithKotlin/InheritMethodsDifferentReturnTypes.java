package test;

public final class InheritMethodsDifferentReturnTypes {
    public interface Super1 {
        CharSequence foo();
        String bar();
    }

    public interface Super2 {
        String foo();
        CharSequence bar();
    }

    public interface Sub extends Super1, Super2 {
    }
}
