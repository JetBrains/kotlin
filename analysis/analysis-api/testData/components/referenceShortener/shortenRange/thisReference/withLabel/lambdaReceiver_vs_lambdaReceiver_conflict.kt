package test

class Regular {
    fun one() {}
}

fun test(action: Regular.() -> Unit) {}

fun otherReceiver(action: Any.() -> Unit) {}

fun usage() {
    test {
        test r2@{
            otherReceiver {
                <expr>this@r2.one()</expr>
            }
        }
    }
}