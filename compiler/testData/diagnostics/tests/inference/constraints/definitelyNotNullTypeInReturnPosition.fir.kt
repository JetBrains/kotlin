// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -CAST_NEVER_SUCCEEDS -UNUSED_VARIABLE -UNCHECKED_CAST

class Foo<T>(x: T)
class Bar<S>
class OutBar<out S>
class InBar<in S>

interface IBar<S>
interface IFoo<S>

typealias OutBarAliasUseSite<T> = Bar<out T>
typealias OutBarAliasDecSite<T> = OutBar<T>

fun <T> materialize(): T = null as T

fun <K> foo0(x: K?): Bar<K> = materialize()
fun <K> foo1(x: K?): Foo<Bar<K>> = materialize()
fun <K, T: K> foo2(x: K?): Foo<Bar<T>> = materialize()
fun <T, K: T> foo3(x: K?): Foo<Bar<T>> = materialize()
fun <K> foo4(x: K?): Foo<Bar<out K>> = materialize()
fun <K> foo5(x: K?): Bar<in K> = materialize()
fun <K> foo6(x: K?): OutBar<K> = materialize()
fun <K> foo7(x: K?): InBar<K> = materialize()
fun <T, K: T, S: K, M: S> foo8(x: T?): Foo<Bar<M>> = materialize()
fun <T, K: T, S: K, M: S> foo9(x: M?): Foo<Bar<T>> = materialize()
fun <T: J, K: T, S: K, M: S, J: L, L> foo10(x: L?, y: Foo<Bar<T>>): Bar<M> = materialize()
fun <T: J, K: T, S: K, M: S, J: L, L> foo11(x: M?, y: Foo<Bar<T>>): Bar<L> = materialize()
fun <K: Any> foo12(x: K?): Bar<K> = materialize()

class Foo13<T>(x: T) {
    fun <K: T> foo1(x: K?): Bar<T> = materialize()
}

fun <K> foo14(x: K?): Bar<K> where K: Comparable<K>, K: CharSequence = materialize()
fun <K: T?, T> foo15(x: T): Bar<K> = materialize()
fun <K: T?, T> foo16(x: K): Bar<T> = materialize()
fun <K: T?, T> foo17(x: K): Bar<T> = null as Bar<T>
fun <K> foo19(x: Bar<K>): K = null as K
fun <K> Bar<K>.foo20(): K = null as K

fun <K> foo21(x: K?): Foo<Foo<OutBar<K>>> = materialize()
fun <K> foo22(x: K?): Foo<Foo<InBar<K>>> = materialize()
fun <K> foo23(x: K?): Foo<Foo<Bar<out K>>> = materialize()
fun <K> foo24(x: K?): Foo<Foo<Bar<in K>>> = materialize()

fun <K> foo25(x: K?): Bar<out K> = materialize()
fun <K> foo26(x: K?): Foo<out Foo<out Bar<out K>>> = materialize()
fun <K> foo27(x: K?): Foo<out Foo<Bar<out K>>> = materialize()
fun <K> foo28(x: K?): OutBar<OutBar<OutBar<K>>> = materialize()
fun <K> foo29(x: K?): OutBar<Bar<OutBar<K>>> = materialize()
fun <K> foo30(x: K?): OutBar<Bar<out OutBar<K>>> = materialize()
fun <K> foo31(x: K?): OutBarAliasUseSite<K> = materialize()
fun <K> foo32(x: K?): OutBarAliasDecSite<K> = materialize()
fun <K> foo33(x: K?): OutBar<InBar<OutBar<K>>> = materialize()
fun <K> foo34(x: K?): OutBar<Bar<in OutBar<K>>> = materialize()
fun <K> foo35(x: K?): InBar<K> = materialize()
fun <K> foo36(x: K?): Bar<in K> = materialize()
fun <K, T: Bar<K>> foo37(x: K?): T = materialize()
fun <K, T: Bar<S>, S: Bar<K>> foo38(x: K?): T = materialize()
fun <K, T: Bar<S>, S: Bar<K>> foo39(x: K?): Bar<T> = materialize()
fun <K, T: Bar<K>> foo40(x: K?): Bar<T> = materialize()
fun <K, T: Bar<K>> foo41(x: K?): T = materialize()
fun <K, S: K, T> foo42(x: K?): T where T: IFoo<S> = materialize()
fun <K, S: K, T> foo43(x: K?): T where T: IBar<S>, T: IFoo<S> = materialize()
fun <K, S, T: S> foo44(x: K?): T where S: IFoo<String>, S: IBar<K> = materialize()
fun <K, T: OutBar<S>, S: Bar<K>> foo45(x: K?): OutBar<T> = materialize()
fun <K, T: OutBar<S>, S: OutBar<K>> foo46(x: K?): Bar<T> = materialize()
fun <K, T: OutBar<S>, S: OutBar<K>> foo47(x: K?): OutBar<T> = materialize()
fun <U: Any> foo48(fn: Function0<U?>): Bar<U> = materialize()

val <K> K?.vfoo0: Foo<Bar<K>> get() = materialize()
val <K> K?.vfoo1: OutBar<Bar<out OutBar<K>>> get() = materialize()
val <K> K?.vfoo2: OutBar<Bar<in OutBar<K>>> get() = materialize()

class Main<L>(x: L?, y: L) {
    init {
        if (x != null && y != null) {
            val x12 = foo1(x)
            val x13 = foo1(y)
        }
        if (x != null && y != null) {
            val x120 = foo12(x)
            val x121 = foo12(y)
        }
        if (x != null) {
            val x137 = Foo13(y).foo1(x)
        }
        if (y != null) {
            val x138 = Foo13(x).foo1(y)
        }
        if (x != null && y != null) {
            val x153 = foo15(x)
            val x154 = foo15(y)
        }
        if (x != null && y != null) {
            val x163 = foo16(x)
            val x164 = foo16(y)
        }
    }

    val x00 = foo0(x)
    val x01 = foo0(y)

    val x10 = foo1(x)
    val x11 = foo1(y)

    val x12 = foo1(x!!)
    val x13 = foo1(y!!)

    val x20 = foo2(x)
    val x21 = foo2(y)

    val x30 = foo3(x)
    val x31 = foo3(y)

    val x40 = foo4(x)
    val x41 = foo4(y)

    val x50 = foo5(x)
    val x51 = foo5(y)

    val x60 = foo6(x)
    val x61 = foo6(y)

    val x70 = foo7(x)
    val x71 = foo7(y)

    val x80 = foo8(x)
    val x81 = foo8(y)

    val x90 = foo9(x)
    val x91 = foo9(y)

    val x100 = foo10(x, Foo(Bar()))
    val x101 = foo10(y, Foo(Bar()))

    val x110 = foo11(x, Foo(Bar()))
    val x111 = foo11(y, Foo(Bar()))

    val x120 = foo12(x!!)
    val x121 = foo12(y!!)

    val x122 = foo12(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    val x123 = foo12(<!ARGUMENT_TYPE_MISMATCH!>y<!>)

    val x133 = Foo13(x).foo1(y)
    val x135 = Foo13(y).foo1(y)
    val x137 = Foo13(y).foo1(x!!)
    val x138 = Foo13(x).foo1(y!!)

    val x140 = foo14("y")
    val x141 = foo14("x")

    val x151 = foo15(x)
    val x152 = foo15(y)
    val x153 = foo15(x!!)
    val x154 = foo15(y!!)

    val x161 = foo16(x)
    val x162 = foo16(y)
    val x163 = foo16(x!!)
    val x164 = foo16(y!!)

    val x170 = foo17(x)
    val x171 = foo17(y)

    val x180 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Bar<!>().<!UNRESOLVED_REFERENCE!>foo18<!>(x)
    val x181 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Bar<!>().<!UNRESOLVED_REFERENCE!>foo18<!>(y)

    val x200: L = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Bar<!>().<!UNRESOLVED_REFERENCE!>foo19<!>()
    val x201: L = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Bar<!>().<!UNRESOLVED_REFERENCE!>foo19<!>()

    val x210 = foo21(x)
    val x211 = foo21(y)

    val x220 = foo22(x)
    val x221 = foo22(y)

    val x230 = foo23(x)
    val x231 = foo23(y)

    val x240 = foo24(x)
    val x241 = foo24(y)

    val x250 = foo25(x)
    val x251 = foo25(y)

    val x260 = foo26(x)
    val x261 = foo26(y)

    val x270 = foo27(x)
    val x271 = foo27(y)

    val x280 = foo28(x)
    val x281 = foo28(y)

    val x290 = foo29(x)
    val x291 = foo29(y)

    val x300 = foo30(x)
    val x301 = foo30(y)

    val x310 = foo31(x)
    val x311 = foo31(y)

    val x320 = foo32(x)
    val x321 = foo32(y)

    val x330 = foo33(x)
    val x331 = foo33(y)

    val x340 = foo34(x)
    val x341 = foo34(y)

    val x350 = foo35(x)
    val x351 = foo35(y)

    val x360 = foo36(x)
    val x361 = foo36(y)

    val vx01 = x.vfoo0
    val vx02 = y.vfoo0

    val vx11 = x.vfoo1
    val vx12 = y.vfoo1

    val vx21 = x.vfoo2
    val vx22 = y.vfoo2

    val x370 = foo37(x)
    val x371 = foo37(y)

    val x380 = foo38(x)
    val x381 = foo38(y)

    val x390 = foo39(x)
    val x391 = foo39(y)

    val x400 = foo40(x)
    val x401 = foo40(y)

    val x410 = foo41(x)
    val x411 = foo41(y)

    val x420 = foo42(x)
    val x421 = foo42(y)

    val x430 = foo43(x)
    val x431 = foo43(y)

    // Change after fix KT-37380
    val x440 = foo44(x)
    val x441 = foo44(y)

    val x450 = foo45(x)
    val x451 = foo45(y)

    val x460 = foo46(x)
    val x461 = foo46(y)

    val x470 = foo47(x)
    val x471 = foo47(y)

    fun <R> takeLambda(block: () -> R): R = materialize()
    val x480 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>takeLambda<!> { foo48 { <!ARGUMENT_TYPE_MISMATCH!>x<!> } }
    val x481 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>takeLambda<!> { foo48 { <!ARGUMENT_TYPE_MISMATCH!>y<!> } }
    val x482 = takeLambda { foo48 { null } }
}

fun <T : Comparable<T>> nullsLast() = null as Foo<T?>
fun <K> take(x: Foo<K>, comparator: Foo<K>): Foo<K> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <L> test() {
    take(null as Foo<String?>, nullsLast())
}

class Inv1<T>
class Inv2<T>
fun <K : Comparable<K>> Inv1<K>.assertStableSorted() {}
fun <K : Comparable<K>> Inv2<K>.assertStableSorted() = Inv1<K>().assertStableSorted()
