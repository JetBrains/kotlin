val z = 30
var x: Int = 0

if (true) {
    fun foo() = z + 20
    x = foo()
}

val rv = x

// expected: rv: 50
