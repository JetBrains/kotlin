// WITH_STDLIB
// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// ISSUE: KT-89847

// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

@JvmInline
value class IC @JvmExposeBoxed constructor(val s: String) {
    @JvmExposeBoxed
    constructor(vararg xs: UInt) : this("O" + xs.sum())
}

@JvmExposeBoxed
class UIntStack(vararg val items: UInt) {
    fun sum(vararg xs: UInt): UInt = items.sum() + xs.sum()
}

@JvmExposeBoxed
class Prefixed(val prefix: IC, vararg val items: UByte) {
    fun result(): String = prefix.s + items.sum()
}

@JvmExposeBoxed
class UIntCollection(private vararg val items: UInt) : Collection<UInt> {
    override val size: Int get() = items.size
    override fun isEmpty(): Boolean = items.isEmpty()
    override fun contains(element: UInt): Boolean = element in items
    override fun containsAll(elements: Collection<UInt>): Boolean = elements.all { it in items }
    override fun iterator(): Iterator<UInt> = items.iterator()
}

open class Base {
    open fun f(vararg xs: UInt): String = "B" + xs.sum()
}

class Derived : Base() {
    @JvmExposeBoxed
    override fun f(vararg xs: UInt): String = "D" + xs.sum()
}

@JvmExposeBoxed
fun notLast(vararg xs: UInt, s: String): String = s + xs.sum()

@JvmExposeBoxed
fun sumULong(vararg xs: ULong): ULong = xs.sum()

@JvmExposeBoxed
fun sumUShort(prefix: String, vararg xs: UShort): String = prefix + xs.sum()

@JvmExposeBoxed("u")
fun createU(i: Int): UInt = i.toUInt()

@JvmExposeBoxed("ul")
fun createUl(i: Int): ULong = i.toULong()

@JvmExposeBoxed("ub")
fun createUb(i: Int): UByte = i.toUByte()

@JvmExposeBoxed("us")
fun createUs(i: Int): UShort = i.toUShort()

@JvmExposeBoxed("uintArray")
fun createUintArray(): UIntArray = uintArrayOf(1u, 2u)

@JvmExposeBoxed("ulongArray")
fun createUlongArray(): ULongArray = ulongArrayOf(1u, 2u)

@JvmExposeBoxed("ubyteArray")
fun createUbyteArray(): UByteArray = ubyteArrayOf(1u, 2u)

@JvmExposeBoxed("ushortArray")
fun createUshortArray(): UShortArray = ushortArrayOf(1u, 2u)

// FILE: Main.java
import kotlin.UInt;

public class Main {
    public static String test() {
        if (!new UIntStack(TestKt.u(1), TestKt.u(2)).sum(TestKt.u(3)).equals(TestKt.u(6))) return "FAIL: ctor vararg";
        if (!new UIntStack(TestKt.uintArray()).sum(TestKt.uintArray()).equals(TestKt.u(6))) return "FAIL: ctor array";
        if (!new UIntStack().sum().equals(TestKt.u(0))) return "FAIL: ctor empty vararg";

        if (!new Prefixed(new IC("O"), TestKt.ub(1), TestKt.ub(2)).result().equals("O3")) return "FAIL: prefixed vararg";
        if (!new Prefixed(new IC("O"), TestKt.ubyteArray()).result().equals("O3")) return "FAIL: prefixed array";

        if (!TestKt.sumULong(TestKt.ul(1), TestKt.ul(2)).equals(TestKt.ul(3))) return "FAIL: fun vararg";
        if (!TestKt.sumULong(TestKt.ulongArray()).equals(TestKt.ul(3))) return "FAIL: fun array";

        if (!TestKt.sumUShort("K", TestKt.us(1), TestKt.us(2)).equals("K3")) return "FAIL: fun prefixed vararg";
        if (!TestKt.sumUShort("K", TestKt.ushortArray()).equals("K3")) return "FAIL: fun prefixed array";

        if (!new IC(TestKt.u(1), TestKt.u(2)).getS().equals("O3")) return "FAIL: value class ctor vararg";
        if (!new IC(TestKt.uintArray()).getS().equals("O3")) return "FAIL: value class ctor array";

        if (new UIntCollection(TestKt.u(1), TestKt.u(2)).size() != 2) return "FAIL: collection vararg";
        if (!new UIntCollection(TestKt.uintArray()).contains(TestKt.u(2))) return "FAIL: collection array";

        if (!new Derived().f(TestKt.u(1), TestKt.u(2)).equals("D3")) return "FAIL: override vararg";
        if (!new Derived().f(TestKt.uintArray()).equals("D3")) return "FAIL: override array";

        if (!TestKt.notLast(new UInt[] { TestKt.u(1), TestKt.u(2) }, "K").equals("K3")) return "FAIL: not last boxed array";
        if (!TestKt.notLast(TestKt.uintArray(), "K").equals("K3")) return "FAIL: not last array";

        return "OK";
    }
}

// FILE: Box.kt
fun box(): String = Main.test()
