// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// ISSUE: KT-28668

// FILE: ZipZap.java
package test;

public class ZipZap {
    public static String result = "";

    public interface Zipper {
        int zip();
    }

    public interface Zapper {
        int zap();
    }

    public int get(Zipper zipper) {
        return zipper.zip();
    }

    public void set(Zapper zapper, int x) {
        result = result + "zap: " + zapper.zap() + "|";
        result = result + "x: " + x;
    }
}

// FILE: main.kt
package test

fun box(): String {
    ZipZap()[{ 42 }]++
    val result = ZipZap.result
    return if (result == "zap: 42|x: 43") "OK" else "Fail: $result"
}
