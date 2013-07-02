package test;

public interface InheritedSameAdapters {
    public interface Super1 {
        void foo(Runnable r);
    }

    public interface Super2 {
        void foo(Runnable r);
    }

    public interface Sub extends Super1, Super2 {
    }
}
