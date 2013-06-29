package test;

public interface SamSubinterfaceOfTwo {
    public interface Super1 {
        CharSequence f();
    }

    public interface Super2<T> {
        T f();
    }

    public interface Sub extends Super1, Super2<String> {
    }
}
