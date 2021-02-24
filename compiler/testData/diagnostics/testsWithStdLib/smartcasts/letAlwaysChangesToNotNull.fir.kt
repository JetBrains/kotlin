// KT-9051: Allow smart cast for captured variables if they are not modified

fun foo(y: String) {
    var x: String? = null
    y.let { x = it }
    x.length // Smart cast is not possible
}
