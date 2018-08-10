class Outer {
    class Nested {
        fun fn(): String  {
            s = "OK"
            return s
        }
    }

    @kotlin.native.ThreadLocal
    companion object {
        public var s = "fail"
            private set
    }
}

fun box(): String {
    return Outer.Nested().fn()
}