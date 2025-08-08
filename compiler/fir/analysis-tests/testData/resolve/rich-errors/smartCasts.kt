// RUN_PIPELINE_TILL: BACKEND

error object E1
error object E2

fun <T : Any? | KError> materialize(): T = null!!

fun <T : Any? | KError> expect(v: T) {}

fun foo0() {
    val tmp = materialize<Int | E1>()
    if (tmp is Int) {
        expect<Int>(tmp)
    } else {
        expect<E1>(tmp)
    }
}

fun foo1() {
    val tmp = materialize<Int | E1 | E2>()
    if (tmp is KError) {
        expect<E1 | E2>(tmp)
    } else {
        expect<Int>(tmp)
    }
}

fun foo2() {
    val tmp = materialize<Int | E1 | E2>()
    if (tmp == E2) return
    expect<Int | E1>(tmp)
    if (tmp is Int) return
    expect<E1>(tmp)
}
