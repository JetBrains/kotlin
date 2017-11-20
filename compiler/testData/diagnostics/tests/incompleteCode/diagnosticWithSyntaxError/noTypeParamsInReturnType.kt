// !WITH_NEW_INFERENCE
package b

fun <T, R> foo(<!UNUSED_PARAMETER!>map<!>: Map<T, R>) : R = throw Exception()

fun <F, G> getMap() : Map<F, G> = throw Exception()

fun bar123() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getMap<!>(
<!SYNTAX!><!>}

