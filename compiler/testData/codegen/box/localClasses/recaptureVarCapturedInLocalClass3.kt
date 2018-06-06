fun box(): String {
    var x = ""

    class CapturesX {
        override fun toString() = x
    }

    class LocalClass {
        fun foo() = CapturesX()
    }

    x = "OK"
    return LocalClass().foo().toString()
}