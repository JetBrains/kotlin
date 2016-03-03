package test

class Test {
    private abstract class Base {
        protected fun duplicate(s: String) = s + "K"

        protected inline fun doInline(block: () -> String): String {
            return duplicate(block())
        }
    }

    private class Extender: Base() {
        fun doSomething(): String {
            return doInline { "O" }
        }
    }

    fun run(): String {
        return Extender().doSomething();
    }
}