// IGNORE_BACKEND_FIR: JVM_IR
class Outer {
    class Nested {
        fun fn(): String  {
            s = "OK"
            return s
        }
    }

    companion object {
        public var s = "fail"
            private set
    }
}

fun box(): String {
    return Outer.Nested().fn()
}