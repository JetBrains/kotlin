package test

class Regular {
    fun one() {}
}

fun test(action: Regular.() -> Unit) {}

fun usage() {
    test {
        <expr>this@test.one()</expr>
    }
}