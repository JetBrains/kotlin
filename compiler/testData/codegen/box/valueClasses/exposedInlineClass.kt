// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// FILE: Example.kt
package example

import interop.*

interface I {
    fun virtualFunction(another: StringWrapper): StringWrapper
}

@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String) : I {
    constructor(i: Int) : this(i.toString())

    init {
        require(s != "")
    }

    fun plainFunction(): Unit = println(s)
    override fun virtualFunction(another: StringWrapper): StringWrapper = StringWrapper(s + another.s)
}

fun topLevelFunction(e: StringWrapper): StringWrapper = e.virtualFunction(e)

@JvmInline
@JvmExposeBoxed
value class IntWrapper(val i: Int) : I {
    constructor(l: Long) : this(l.toInt())

    init {
        require(i != 0)
    }

    fun plainFunction(another: IntWrapper): IntWrapper = IntWrapper(i + another.i)

    override fun virtualFunction(another: StringWrapper): StringWrapper = StringWrapper(i.toString() + another.s)
}

fun topLevelFunction(e: IntWrapper): IntWrapper = e.plainFunction(e)

fun assertEquals(value1: Any?, value2: Any?) {
    if (value1 != value2)
        throw AssertionError("Expected $value1, got $value2")
}

fun box(): String {
    val w = IntWrapper(42L)
    assertEquals(IntWrapper(84), w.plainFunction(w))
    val x = StringWrapper(42)
    assertEquals(StringWrapper("4242"), x.virtualFunction(x))
    assertEquals(StringWrapper("4242"), w.virtualFunction(x))
    assertEquals(IntWrapper(84), topLevelFunction(w))
    assertEquals(StringWrapper("4242"), topLevelFunction(x))
    Interop.main()
    return "OK"
}

// FILE: interop/Interop.java
package interop;

import example.*;

public class Interop {
    public static void main() {
        IntWrapper wrp = new IntWrapper(42L);
        ExampleKt.assertEquals(new IntWrapper(84), wrp.plainFunction(wrp));
        StringWrapper x = new StringWrapper(42);
        ExampleKt.assertEquals(new StringWrapper("4242"), x.virtualFunction(x));
        ExampleKt.assertEquals(new StringWrapper("4242"), wrp.virtualFunction(x));
        ExampleKt.assertEquals(new IntWrapper(84L), ExampleKt.topLevelFunction(wrp));
        ExampleKt.assertEquals(new StringWrapper("4242"), ExampleKt.topLevelFunction(x));
    }
}
