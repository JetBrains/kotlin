package a

fun fooT2<T>() : (t : T) -> T {
    return {it}
}

fun test() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>fooT2<!>()(1) // here 1 should not be marked with an error
}