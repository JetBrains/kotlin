// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -UNUSED_VARIABLE

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
    foo0(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
    foo0(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)

    foo1(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)
    foo1(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)

    if (x != null && y != null) {
        foo1(<!DEBUG_INFO_EXPRESSION_TYPE("L & Any & L?")!>x<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)
        foo1(<!DEBUG_INFO_EXPRESSION_TYPE("L & L & Any")!>y<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)
    }

    foo2(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)
    foo2(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)

    foo3(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)
    foo3(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)

    foo4(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<out L & Any>>")!>Foo(Bar())<!>)
    foo4(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<out L & Any>>")!>Foo(Bar())<!>)

    foo5(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
    foo5(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)

    foo6(x, <!DEBUG_INFO_EXPRESSION_TYPE("OutBar<L & Any>")!>OutBar()<!>)
    foo6(y, <!DEBUG_INFO_EXPRESSION_TYPE("OutBar<L & Any>")!>OutBar()<!>)

    foo7(x, <!DEBUG_INFO_EXPRESSION_TYPE("InBar<L & Any>")!>InBar()<!>)
    foo7(y, <!DEBUG_INFO_EXPRESSION_TYPE("InBar<L & Any>")!>InBar()<!>)

    foo8(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)
    foo8(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)

    foo9(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)
    foo9(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>)

    foo10(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
    foo10(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)

    foo11(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
    foo11(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Bar<L & Any>>")!>Foo(Bar())<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)

    if (x != null && y != null) {
        foo12(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
        foo12(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
    }

    foo12(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
    foo12(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)

    Foo13(x).foo1(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L?>")!>Bar()<!>)
    Foo13(x).foo2(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L?>")!>Bar()<!>)
    Foo13(y).foo1(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L>")!>Bar()<!>)
    Foo13(y).foo2(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L>")!>Bar()<!>)
    if (x != null) {
        Foo13(<!DEBUG_INFO_SMARTCAST!>x<!>).foo2(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
        Foo13(y).foo2(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L>")!>Bar()<!>)
    }
    if (y != null) {
        Foo13(x).foo2(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L?>")!>Bar()<!>)
        Foo13(<!DEBUG_INFO_SMARTCAST!>y<!>).foo2(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
    }

    foo14("y", <!DEBUG_INFO_EXPRESSION_TYPE("Bar<kotlin.String>")!>Bar()<!>)
    foo14("x", <!DEBUG_INFO_EXPRESSION_TYPE("Bar<kotlin.String>")!>Bar()<!>)

    foo15(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L?>")!>Bar()<!>)
    foo15(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L?>")!>Bar()<!>)
    if (x != null && y != null) {
        foo15(<!DEBUG_INFO_SMARTCAST!>x<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L?>")!>Bar()<!>)
        foo15(<!DEBUG_INFO_SMARTCAST!>y<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L?>")!>Bar()<!>)
    }

    foo16(x, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L>")!>Bar()<!>)
    foo16(y, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L>")!>Bar()<!>)
    if (x != null && y != null) {
        foo16(<!DEBUG_INFO_SMARTCAST!>x<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
        foo16(<!DEBUG_INFO_SMARTCAST!>y<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Bar<L & Any>")!>Bar()<!>)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Bar<!>()<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo18<!>(x)
    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Bar<!>()<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo18<!>(y)

    foo21(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Foo<OutBar<L & Any>>>")!>Foo(Foo(OutBar()))<!>)
    foo21(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Foo<OutBar<L & Any>>>")!>Foo(Foo(OutBar()))<!>)

    foo22(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Foo<InBar<L & Any>>>")!>Foo(Foo(InBar()))<!>)
    foo22(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Foo<InBar<L & Any>>>")!>Foo(Foo(InBar()))<!>)

    foo23(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Foo<Bar<out L & Any>>>")!>Foo(Foo(Bar()))<!>)
    foo23(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Foo<Bar<out L & Any>>>")!>Foo(Foo(Bar()))<!>)

    foo24(x, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Foo<Bar<in L & Any>>>")!>Foo(Foo(Bar()))<!>)
    foo24(y, <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Foo<Bar<in L & Any>>>")!>Foo(Foo(Bar()))<!>)
}
