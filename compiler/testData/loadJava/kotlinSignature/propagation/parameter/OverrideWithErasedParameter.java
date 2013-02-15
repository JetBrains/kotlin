package test;

public interface OverrideWithErasedParameter  {

    public interface Super<T> {
        void foo(T t);
    }

    public interface Sub<T> extends Super<T> {
        void foo(Object o);
    }
}