package serialization.fake_overrides

open class X {
}

class Y: X() {
    fun bar() = "barStale"
}

class B: A() {
}

class C: A() {
    override fun tic() = "ticChild"
}

