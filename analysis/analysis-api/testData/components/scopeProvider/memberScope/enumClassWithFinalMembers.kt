package test

enum class E {
    A {
        val x: String = ""
    },
    B;

    val foo: Int = 5

    fun bar() {
        println(foo)
    }
}

// class: test/E
