// !WITH_NEW_INFERENCE

class ExcA : Exception()

class ExcB(val map: Map<Int, Int>) : Exception()

fun test0(): List<Int> = run {
    try {
        emptyList()
    } finally {
        <!UNUSED_EXPRESSION!>""<!>
        fun foo() {}
    }
}

fun test1(): Map<Int, Int> = <!NI;TYPE_MISMATCH!>run {
    <!NI;TYPE_MISMATCH!>try {
        emptyMap()
    } catch (e: ExcA) {
        emptyMap()
    } catch (e: ExcB) {
        e.map
    } finally {
        <!UNUSED_EXPRESSION!>""<!>
    }<!>
}<!>

fun test2(): Map<Int, Int> = <!NI;TYPE_MISMATCH!>run {
    <!NI;TYPE_MISMATCH!>try {
        emptyMap()
    } catch (e: ExcA) {
        <!OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>mapOf("" to "")<!>
    }<!>
}<!>