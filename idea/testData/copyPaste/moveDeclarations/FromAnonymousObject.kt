// IS_AVAILABLE: false

fun foo() {
    val v = object : Runnable {
        override fun run() {
        }

<selection>
        fun bar() {
        }
</selection>
    }
}

<caret>
