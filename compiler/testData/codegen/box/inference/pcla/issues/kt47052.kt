// WITH_STDLIB

public inline fun <R, C : MutableCollection<in R>> flatMapTo1(destination: C, transform: (List<String>) -> Iterable<R>) {}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    buildSet { // NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER is reported
        flatMapTo1(this) { it }
    }
    return "OK"
}
