package test;

public interface InheritedSimple {
    public interface Super {
        void foo(Runnable r);
    }

    public interface Sub extends Super {
    }
}
