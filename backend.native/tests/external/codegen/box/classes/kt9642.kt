class Outer {
    class Nested {
        fun fn(): String  {
            s = "OK"
            return s
        }
    }

    @konan.ThreadLocal
    companion object {
        public var s = "fail"
            private set
    }
}

fun box(): String {
    return Outer.Nested().fn()
}