// WITH_STDLIB
// SKIP_TXT

fun List<Int>.myExt() {}
fun <T> List<T>.myGenericExt() {}

fun <R> a(first: R, second: (List<R>) -> Unit) {}

fun test1() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>a<!>(
        buildList { add("") },
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION("R; a")!>second = {
            it.myGenericExt()
        }<!>
    )
}


fun <R> b(first: () -> List<R>, second: (List<R>) -> Unit) {}

fun test2() {
    b(
        first = {
            buildList { add("") }
        },
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION("R; b")!>second = {
            it.myExt() // Note: must be extension to add constraints
        }<!>
    )
}

fun <Q> select(a: Q, b: Q): Q = a

// Note: no builder inference annotation
fun <R> myBuildList(builder: MutableList<R>.() -> Unit): List<R> {
    val list = mutableListOf<R>()
    list.builder()
    return list
}

fun test3() {
    select(
        buildList { add("") },
        buildList { add(1) }
    )

    select (
        myBuildList <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION("R; myBuildList")!>{ add("") }<!>,
        myBuildList <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION("R; myBuildList")!>{ add(1) }<!>,
    )

    select (
        run { myBuildList <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION("R; myBuildList")!>{ add("") }<!> },
        myBuildList <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION("R; myBuildList")!>{ add(1) }<!>,
    )
}

fun <D> buildPartList(left: MutableList<D>.() -> Unit, right: MutableList<D>.() -> Unit): List<D> {
    val list = mutableListOf<D>()
    list.left()
    list.right()
    return list
}

fun test4() {
    buildPartList(
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION("D; buildPartList")!>left = { add(1) }<!>,
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION("D; buildPartList")!>right = { add("") }<!>
    )
}
