package test;

import java.io.Closeable;

public interface InheritedAmbiguousAdapters {
    public interface Super {
        void foo(Runnable r);
        void foo(Closeable r);
    }

    public interface Sub extends Super {
    }
}
