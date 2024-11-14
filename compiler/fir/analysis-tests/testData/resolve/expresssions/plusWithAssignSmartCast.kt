// RUN_PIPELINE_TILL: BACKEND
class Foo {
    var bar: String? = null

    fun addToBar(other: String) {
        if (bar == null) {
            bar = other
        } else {
            bar += " $other"
        }
    }
}