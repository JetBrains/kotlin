enum class Direction() {
    NORTH {
        val someSpecialValue = "OK"

        override fun f() = someSpecialValue
    };


    abstract fun f():String
}

fun box() = Direction.NORTH.f()
