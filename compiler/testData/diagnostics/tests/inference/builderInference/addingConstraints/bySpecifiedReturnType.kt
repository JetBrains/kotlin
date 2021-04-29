// WITH_RUNTIME

interface A {
    fun foo(): MutableList<String>
}

@ExperimentalStdlibApi
fun main() {
    buildList {
        add(<!CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH!>3<!>)
        object : A {
            override fun foo(): MutableList<String> = this@buildList
        }
    }
    buildList {
        add(<!CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH!>3<!>)
        val x: String = get(0)
    }
    buildList {
        add(<!TYPE_MISMATCH, TYPE_MISMATCH!>"3"<!>)
        val x: MutableList<Int> = this@buildList
    }
    buildList {
        val y: CharSequence = ""
        add(<!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>y<!>)
        val x: MutableList<String> = this@buildList
    }
    buildList {
        add("")
        val x: MutableList<CharSequence> = this@buildList
    }
    buildList {
        add(<!TYPE_MISMATCH, TYPE_MISMATCH!>""<!>)
        val x: StringBuilder = get(0)
    }
}
