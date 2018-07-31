fun println(obj: Any?) {}

class Demo0 {
    private val some = object {
        fun foo() {
            println(state) // Ok
        }
    }

    private var state: Boolean = true
}

class Demo1 {
    private val some = object {
        fun foo() {
            if (state)
                state = true

            println(state) // must be initialized
        }
    }

    private var state: Boolean = true
}

class Demo1A {
    fun foo() {
        if (state)
            state = true

        println(state) // Ok
    }

    private var state: Boolean = true
}

class Demo2 {
    private val some = object {
        fun foo() {
            if (state)
                state = true
            else
                state = false

            println(state) // OK
        }
    }

    private var state: Boolean = true
}

class Demo3 {
    private val some = run {
        if (state)
            state = true

        println(state) // OK
    }

    private var state: Boolean = true
}

fun <T, R> T.run(f: T.() -> R) = f()
fun <T> exec(f: () -> T): T = f()

class Demo4 {
    private val some = exec {
        if (state)
            state = true

        println(state) // must be initialized
    }

    private var state: Boolean = true
}

class Demo5 {
    private var state: Boolean = true

    private val some = object {
        fun foo() {
            if (state)
                state = true

            println(state) // OK
        }
    }
}
