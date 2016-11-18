class C {
    val f = object : Runnable {
        fun run() { }

        fun xyzzy() { }
    }

    fun bar() {
        val g = object : Runnable {
            fun run() { }

            fun xyzzy() { }
        }
    }
}