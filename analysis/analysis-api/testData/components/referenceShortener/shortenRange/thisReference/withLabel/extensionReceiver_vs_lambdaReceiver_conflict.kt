package test

class Regular {
    fun one() {}
}

fun test(action: Regular.() -> Unit) {}

fun Regular.usage() {
    test {
        <expr>this@usage.one()</expr>
    }
}