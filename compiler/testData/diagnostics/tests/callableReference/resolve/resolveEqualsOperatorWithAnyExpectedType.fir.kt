// !LANGUAGE: +NewInference

interface Base

fun <K> materialize(): K = TODO()

fun <T : Base> Base.transform(): T = materialize()

fun test(child: Base) {
    child == child.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>transform<!>()
}
