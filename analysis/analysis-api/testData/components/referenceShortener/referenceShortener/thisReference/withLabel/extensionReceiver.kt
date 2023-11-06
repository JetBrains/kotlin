package test

class Regular {
    fun one() {}
}

fun Regular.usage() {
    <expr>this@usage.one()</expr>
}