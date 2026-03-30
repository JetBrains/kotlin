// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

open class Props {
    val a: Int = 0
    val b: String = ""
}

class ScopeHost {
    fun fromType(...Props.$props) {
        a + 1
        b.length
    }
}

class GenericHost {
    fun <T : Props> fromTypeParameter(...T.$props) {
        a + 1
        b.length
    }
}

class Host : Props() {
    fun source(a: Int, b: String) {}

    fun fromFunction(...source.$props) {
        a + 1
        b.length
    }

    fun target(a: Int, b: String) {}

    fun useBound() {
        target(...Props.$props(this))
        target(...Props.$props(this).exclude(a), a = 1)
    }
}

fun topTarget(a: Int, b: String) {}

fun topLevel() {
    topTarget(...Props.$props(<!NO_THIS!>this<!>))
}
