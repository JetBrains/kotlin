package test;

import java.io.Closeable;

public interface OverriddenAmbiguousAdapters {
    public interface Super {
        void foo(Runnable r);
        void foo(Closeable r);
    }

    public interface Sub extends Super {
        void foo(kotlin.jvm.functions.Function0<kotlin.Unit> r);
    }
}
