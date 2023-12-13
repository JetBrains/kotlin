package test

class MyClass {
    companion object {
        val prop = ""
    }

    object Other {
        val prop = ""

        fun usage() {
            println(<expr>MyClass.prop</expr>)
        }
    }
}