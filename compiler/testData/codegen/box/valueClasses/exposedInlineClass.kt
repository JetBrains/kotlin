// WITH_STDLIB
// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// FILE: Example.kt
@file:OptIn(ExperimentalStdlibApi::class)

package example

import interop.*

@Target(AnnotationTarget.CONSTRUCTOR)
annotation class A

interface I<T> {
    fun virtualFunction(another: T): T
}

@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String) : I<StringWrapper> {
    @A
    constructor(i: Int) : this(i.toString())

    init {
        require(s != "")
    }

    fun plainFunction(): Unit = println(s)
    override fun virtualFunction(another: StringWrapper): StringWrapper = StringWrapper(s + another.s)
}

fun topLevelFunction(e: StringWrapper): StringWrapper = e.virtualFunction(e)

suspend fun suspendFunction(e: StringWrapper): StringWrapper = e.virtualFunction(e)

@JvmInline
@JvmExposeBoxed
value class IntWrapper(val i: Int) : I<IntWrapper> {
    constructor(l: Long) : this(l.toInt())

    init {
        require(i != 0)
    }

    fun plainFunction(another: IntWrapper): IntWrapper = IntWrapper(i + another.i)

    fun IntWrapper.extensionFunction(another: IntWrapper): IntWrapper = IntWrapper(this.i + this@IntWrapper.i + another.i)

    fun StringWrapper.extensionFunction(another: IntWrapper): IntWrapper = IntWrapper(another.i)

    override fun virtualFunction(another: IntWrapper): IntWrapper = IntWrapper(i + another.i)
}

fun topLevelFunction(e: IntWrapper): IntWrapper = e.plainFunction(e)

fun IntWrapper.topLevelFunction(e: IntWrapper): IntWrapper = when {
    e.i > 100 -> IntWrapper(42)
    else -> e.plainFunction(e)
}

fun assertEquals(value1: Any?, value2: Any?) {
    if (value1 != value2)
        throw AssertionError("Expected $value1, got $value2")
}

data class HasWrappers(val i: IntWrapper = IntWrapper(42), val j: StringWrapper = StringWrapper(84))

fun box(): String {
    val intWrapper = IntWrapper(42L)
    assertEquals(IntWrapper(84), intWrapper.plainFunction(intWrapper))
    val stringWrapper = StringWrapper(42)
    assertEquals(StringWrapper("4242"), stringWrapper.virtualFunction(stringWrapper))
    assertEquals(IntWrapper(84), intWrapper.virtualFunction(intWrapper))
    assertEquals(IntWrapper(84), topLevelFunction(intWrapper))
    assertEquals(StringWrapper("4242"), topLevelFunction(stringWrapper))
    val h = HasWrappers(intWrapper)
    assertEquals(intWrapper, h.i)
    Interop.main()
    return "OK"
}

// FILE: interop/Interop.java
package interop;

import example.*;

public class Interop {
    public static void main() {
        IntWrapper intWrapper = new IntWrapper(42L);
        ExampleKt.assertEquals(new IntWrapper(84), intWrapper.plainFunction(intWrapper));
        StringWrapper stringWrapper = new StringWrapper(42);
        ExampleKt.assertEquals(new StringWrapper("4242"), stringWrapper.virtualFunction(stringWrapper));
        ExampleKt.assertEquals(new IntWrapper(84), intWrapper.virtualFunction(intWrapper));
        ExampleKt.assertEquals(new IntWrapper(84L), ExampleKt.topLevelFunction(intWrapper));
        ExampleKt.assertEquals(new StringWrapper("4242"), ExampleKt.topLevelFunction(stringWrapper));
        HasWrappers h = new HasWrappers(intWrapper, stringWrapper);
        ExampleKt.assertEquals(intWrapper, h.getI());
        ExampleKt.assertEquals(stringWrapper, h.getJ());
    }
}
