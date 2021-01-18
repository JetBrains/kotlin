val b: Boolean = true
val c: String = "true"
val d: Int = 1
val e: Long = 1L
val f: Boolean = true

fun foo(): Boolean

fun test(): Int {
    return <caret>
}

//ORDER: test, d