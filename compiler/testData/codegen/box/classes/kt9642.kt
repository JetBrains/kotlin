// JVM_ABI_K1_K2_DIFF: KT-63984

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