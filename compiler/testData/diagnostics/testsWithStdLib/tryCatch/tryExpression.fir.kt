// !WITH_NEW_INFERENCE
// SKIP_TXT

class ExcA : Exception()

class ExcB(val map: Map<Int, Int>) : Exception()

fun test0(): List<Int> = run {
    try {
        emptyList()
    } finally {
        ""
        fun foo() {}
    }
}

fun test1(): Map<Int, Int> = run {
    try {
        emptyMap()
    } catch (e: ExcA) {
        emptyMap()
    } catch (e: ExcB) {
        e.map
    } finally {
        ""
    }
}

fun test2(): Map<Int, Int> = run {
    <!ARGUMENT_TYPE_MISMATCH!>try {
        emptyMap()
    } catch (e: ExcA) {
        mapOf("" to "")
    }<!>
}
