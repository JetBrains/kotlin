// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-91583
// Java annotation arguments copied to delegated members must be converted to the element type
// after the binary numeric promotion of the expression (JLS 5.6, 9.7.1).

// FILE: p/Nums.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nums {
    int i();
    long l();
    float f();
    double d();
    char c();
    short s();
    byte b();
    String str();
    double[] ds();
    long[] ls();
}

// FILE: p/Api.java
package p;

public interface Api {
    @Nums(
        i = 'a' + 1,
        l = 'b',
        f = 1,
        d = 2L,
        c = 'a' + 2,
        s = -(byte) 1,
        b = ~(byte) 1,
        str = "" + ('a' + 1),
        ds = {1, 'a', 2.5f},
        ls = {1, 'b'}
    )
    String foo();
}

// FILE: p/Consts.java
package p;

public class Consts {
    public static final byte B = 1;
    public static final int NEG = -B;
    public static final int INV = ~B;
    public static final String S = "" + ('a' + 1);
}

// FILE: test.kt
import p.*

class Delegator(api: Api) : Api by api

const val NEG = Consts.NEG
const val INV = Consts.INV
const val S = Consts.S

fun box(): String {
    val nums = Delegator::class.java.getMethod("foo").getAnnotation(Nums::class.java)
        ?: return "FAIL: no annotation"
    val actual = listOf(
        nums.i, nums.l, nums.f, nums.d, nums.c, nums.s, nums.b, nums.str, nums.ds.toList(), nums.ls.toList()
    )
    val expected = listOf<Any>(
        98, 98L, 1.0f, 2.0, 'c', (-1).toShort(), (-2).toByte(), "98", listOf(1.0, 97.0, 2.5), listOf(1L, 98L)
    )
    if (actual != expected) return "FAIL: $actual"
    return if (NEG == -1 && INV == -2 && S == "98") "OK" else "FAIL: NEG=$NEG INV=$INV S=$S"
}
