package test

class Other {
    fun one() {}
}

class Regular {
    fun one() {}

    fun Other.usage() {
        <expr>this@Regular.one()</expr>
    }
}