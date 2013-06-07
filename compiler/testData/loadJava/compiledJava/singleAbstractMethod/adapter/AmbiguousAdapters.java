package test;

import java.io.Closeable;

public class AmbiguousAdapters {
    public void foo(Runnable r) {
    }

    public void foo(Closeable c) {
    }
}
