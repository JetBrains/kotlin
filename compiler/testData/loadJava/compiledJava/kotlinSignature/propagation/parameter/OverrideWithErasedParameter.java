package test;

public interface OverrideWithErasedParameter  {

    public interface Super<T> {
        void foo(T t);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub<T> extends Super<T> {
        void foo(Object o);
    }
}
