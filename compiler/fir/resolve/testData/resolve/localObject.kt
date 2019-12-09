fun <T> run(block: () -> T): T = block()

interface Foo {
    fun foo(): Int
}

fun tesLambda(x: Int) = run {
    val obj = object : Foo {
        override fun foo(): Int {
            return x + 1
        }
    }
    2
}

class TestProperty {
    val intConst: Int = 1

    var x = 1
        set(value) {
            val obj = object : Foo {
                override fun foo(): Int {
                    return intConst + 1
                }
            }
            field = value
        }

    val y: Int
        get() {
            val obj = object : Foo {
                override fun foo(): Int {
                    return intConst + 1
                }
            }
            return 1
        }

    val z = run {
        val obj = object : Foo {
            override fun foo(): Int {
                return x + 1
            }
        }
        2
    }
}