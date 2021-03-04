// !WITH_NEW_INFERENCE
package b

fun <T, R> foo(map: Map<T, R>) : R = throw Exception()

fun <F, G> getMap() : Map<F, G> = throw Exception()

fun bar123() {
    foo(<!TOO_MANY_ARGUMENTS!>getMap<!>(
<!SYNTAX!><!>}
