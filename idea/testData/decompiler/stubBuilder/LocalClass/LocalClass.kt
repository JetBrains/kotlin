package test

class LocalClass {
    private fun foo() = run {
        class Local

        Local()
    }

    private val bar = object {}

    private val sam = Runnable {}

    private val sub = object : Runnable {
        override fun run() {
        }
    }
}
