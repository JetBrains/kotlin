// RUNTIME

package baz

@DslMarker
annotation class Marker

@Marker
class A {
    val booFromA = ""

    fun booFromA2() {}

    fun section(block: B.() -> Unit) {}
}

@Marker
class B {
    fun booFromB() {}
}

fun x() {
    A().apply {
        section {
            boo<caret>
        }
    }

}

fun booUnrelated() {

}

// ORDER: booFromB, booUnrelated, booleanArrayOf, booExtension