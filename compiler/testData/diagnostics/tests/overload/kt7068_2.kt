// !DIAGNOSTICS: -UNUSED_PARAMETER

fun withLambda(block : Int.(String) -> Unit) {
}

fun withLambda(block : Int.(String, String) -> Unit) {
}

fun test() {
    withLambda { r ->
        r.length
    }

    withLambda { x, y ->
        x.length + y.length
    }
}