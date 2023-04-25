// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    constructor(p: Any)

    fun f1(s: String): Int

    <!NO_ACTUAL_FOR_EXPECT{JVM}!>fun f2(s: List<String>?): MutableMap<Boolean?, Foo><!>

    fun <T : Set<Number>> f3(t: T): T?
}

// MODULE: m2-jvm()()(m1-common)
// FILE: FooImpl.java

import java.util.*;

public class FooImpl {
    public FooImpl(Object p) {}

    public final int f1(String s) { return 0; }

    public final Map<Boolean, FooImpl> f2(List<String> s) { return null; }

    public final <T extends Set<Number>> T f3(T t) { return null; }
}

// FILE: jvm.kt

actual typealias Foo = FooImpl
