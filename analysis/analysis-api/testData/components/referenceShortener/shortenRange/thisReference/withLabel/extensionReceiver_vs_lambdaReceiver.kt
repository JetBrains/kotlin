package test

class Regular {
    fun one() {}
}

fun test(action: Regular.() -> Unit) {}

fun otherReceiver(action: Any.() -> Unit) {}

fun Regular.usage() {
    test {
        otherReceiver {
            <expr>this@test.one()</expr>
        }
    }
}