// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT
interface Generic<K, V>

fun <X, Y> Generic<X, Y>.getValue(x: X): Y = TODO()

class MyPair<A, B>(a: A, b: B)

fun <E, F> foo(x: Generic<E, F>, e: E, c: Generic<Int, String>): MyPair<F, F> {
    if (c === x && e is Int) {
        bar(MyPair(<!DEBUG_INFO_SMARTCAST!>x<!>.getValue(<!DEBUG_INFO_SMARTCAST!>e<!>), <!DEBUG_INFO_SMARTCAST!>x<!>.getValue(<!DEBUG_INFO_SMARTCAST!>e<!>)))
        return <!TYPE_MISMATCH!>MyPair(<!DEBUG_INFO_SMARTCAST!>x<!>.getValue(<!DEBUG_INFO_SMARTCAST!>e<!>), <!DEBUG_INFO_SMARTCAST!>x<!>.getValue(<!DEBUG_INFO_SMARTCAST!>e<!>))<!>
    }

    return MyPair(x.getValue(e), x.getValue(e))
}

fun bar(p: MyPair<String, String>) {}
