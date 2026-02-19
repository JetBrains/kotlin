package test

class Regular {
    fun one() {}

    inner class Inner {
        fun one() {}

        fun usage() {
            <expr>this@Regular.one()</expr>
        }
    }
}