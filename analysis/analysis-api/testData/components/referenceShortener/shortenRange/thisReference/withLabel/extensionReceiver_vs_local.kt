package test

class Regular {
    fun one() {}
}

fun Regular.usage() {
    fun Regular.local() {
        <expr>this@usage.one()</expr>
    }
}