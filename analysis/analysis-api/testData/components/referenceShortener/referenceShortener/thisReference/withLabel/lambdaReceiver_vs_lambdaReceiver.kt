package test

class Regular {
    fun one() {}
}

fun test(action: Regular.() -> Unit) {}

fun usage() {
    test {
        test r2@{
            <expr>this@test.one()</expr>
        }
    }
}