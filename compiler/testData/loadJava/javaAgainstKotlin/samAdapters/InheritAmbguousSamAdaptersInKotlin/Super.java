package test;

import java.io.Closeable;

public class Super {
    void foo(Runnable r);
    void foo(Closeable r);
}
