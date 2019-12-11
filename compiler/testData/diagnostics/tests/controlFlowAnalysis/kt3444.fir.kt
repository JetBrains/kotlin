fun box() {
    fun local():Int {
    }
}

interface X {
    fun f(): Boolean
}

val m = object : X {
    override fun f(): Boolean {
    }
}