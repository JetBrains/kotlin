// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExpectedTypeGuidedResolution

interface Thing<A> {
    fun add(x: A): Unit
    val last: A?
}

fun <A> buildThing(builder: Thing<A>.() -> Unit): Int = TODO()

class Duration(val milliseconds: Int) {
    companion object {
        val Int.seconds: Duration get() = Duration(this)
    }
}

val x: Int = buildThing {
    add(Duration(0))
    when (last) {
        null -> { }
        1.<!UNRESOLVED_REFERENCE!>seconds<!> -> { }
        else -> { }
    }
}