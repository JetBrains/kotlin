fun foo() = 42
val prop = 42

class Bar {
    fun member() {

    }

    val memberProperty: String
        get() {
            fun b() = "sre"
            return b()
        }
}

foo()