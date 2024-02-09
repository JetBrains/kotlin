// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -UNUSED_VARIABLE

class Foo<T>(x: T)
class Bar<S>
class OutBar<out S>
class InBar<in S>

fun <K> foo0(x: K?, y: Bar<K>) {}
fun <K> foo1(x: K?, y: Foo<Bar<K>>) {}
fun <K, T: K> foo2(x: K?, y: Foo<Bar<T>>) {}
fun <T, K: T> foo3(x: K?, y: Foo<Bar<T>>) {}
fun <K> foo4(x: K?, y: Foo<Bar<out K>>) {}
fun <K> foo5(x: K?, y: Bar<in K>) {}
fun <K> foo6(x: K?, y: OutBar<K>) {}
fun <K> foo7(x: K?, y: InBar<K>) {}
fun <T, K: T, S: K, M: S> foo8(x: T?, y: Foo<Bar<M>>) {}
fun <T, K: T, S: K, M: S> foo9(x: M?, y: Foo<Bar<T>>) {}
fun <T: J, K: T, S: K, M: S, J: L, L> foo10(x: L?, y: Foo<Bar<T>>, z: Bar<M>) {}
fun <T: J, K: T, S: K, M: S, J: L, L> foo11(x: M?, y: Foo<Bar<T>>, z: Bar<L>) {}
fun <K: Any> foo12(x: K?, y: Bar<K>) {}

class Foo13<T>(x: T) {
    fun <K: T> foo1(x: T?, y: Bar<K>) {}
    fun <K: T> foo2(x: K?, y: Bar<T>) {}
}

fun <K> foo14(x: K?, y: Bar<K>) where K: Comparable<K>, K: CharSequence {}
fun <K: T?, T> foo15(x: T, y: Bar<K>) {}
fun <K: T?, T> foo16(x: K, y: Bar<T>) {}
fun <K: T?, T> Bar<K>.foo18(x: T) {}

fun <K> foo21(x: K?, y: Foo<Foo<OutBar<K>>>) {}
fun <K> foo22(x: K?, y: Foo<Foo<InBar<K>>>) {}
fun <K> foo23(x: K?, y: Foo<Foo<Bar<out K>>>) {}
fun <K> foo24(x: K?, y: Foo<Foo<Bar<in K>>>) {}

fun <L> main(x: L?, y: L) {
    foo0(x, Bar())
    foo0(y, Bar())

    foo1(x, Foo(Bar()))
    foo1(y, Foo(Bar()))

    if (x != null && y != null) {
        foo1(x, Foo(Bar()))
        foo1(y, Foo(Bar()))
    }

    foo2(x, Foo(Bar()))
    foo2(y, Foo(Bar()))

    foo3(x, Foo(Bar()))
    foo3(y, Foo(Bar()))

    foo4(x, Foo(Bar()))
    foo4(y, Foo(Bar()))

    foo5(x, Bar())
    foo5(y, Bar())

    foo6(x, OutBar())
    foo6(y, OutBar())

    foo7(x, InBar())
    foo7(y, InBar())

    foo8(x, Foo(Bar()))
    foo8(y, Foo(Bar()))

    foo9(x, Foo(Bar()))
    foo9(y, Foo(Bar()))

    foo10(x, Foo(Bar()), Bar())
    foo10(y, Foo(Bar()), Bar())

    foo11(x, Foo(Bar()), Bar())
    foo11(y, Foo(Bar()), Bar())

    if (x != null && y != null) {
        foo12(x, Bar())
        foo12(y, Bar())
    }

    foo12(x, Bar())
    foo12(y, Bar())

    Foo13(x).foo1(x, Bar())
    Foo13(x).foo2(y, Bar())
    Foo13(y).foo1(x, Bar())
    Foo13(y).foo2(y, Bar())
    if (x != null) {
        Foo13(x).foo2(y, Bar())
        Foo13(y).foo2(x, Bar())
    }
    if (y != null) {
        Foo13(x).foo2(y, Bar())
        Foo13(y).foo2(x, Bar())
    }

    foo14("y", Bar())
    foo14("x", Bar())

    foo15(x, Bar())
    foo15(y, Bar())
    if (x != null && y != null) {
        foo15(x, Bar())
        foo15(y, Bar())
    }

    foo16(x, Bar())
    foo16(y, Bar())
    if (x != null && y != null) {
        foo16(x, Bar())
        foo16(y, Bar())
    }

    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Bar<!>().foo18(x)
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Bar<!>().foo18(y)

    foo21(x, Foo(Foo(OutBar())))
    foo21(y, Foo(Foo(OutBar())))

    foo22(x, Foo(Foo(InBar())))
    foo22(y, Foo(Foo(InBar())))

    foo23(x, Foo(Foo(Bar())))
    foo23(y, Foo(Foo(Bar())))

    foo24(x, Foo(Foo(Bar())))
    foo24(y, Foo(Foo(Bar())))
}
