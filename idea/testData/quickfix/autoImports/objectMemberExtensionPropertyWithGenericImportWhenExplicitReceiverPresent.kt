// "Import" "true"
package p

class T

object TopLevelObject1 {
    val <A> A.foobar get() = 10
}

fun usage(t: T) {
    t.<caret>foobar
}