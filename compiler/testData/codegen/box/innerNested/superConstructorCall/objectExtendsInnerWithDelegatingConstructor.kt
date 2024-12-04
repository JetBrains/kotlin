class Outer(val outer: String) {
    open inner class Inner(val inner: String) {
        constructor() : this("K")

        fun test() = outer + inner
    }

    fun obj(): Inner =
        object : Inner() {}
}

fun box() = Outer("O").obj().test()

// CHECK_BYTECODE_TEXT
// 1 PUTFIELD Outer\$Inner\.this\$0 : LOuter;
//  ^ Outer$Inner.this$0 SHOULD be written in primary constructor of Outer$Inner,
//    and SHOULD NOT be written in delegating constructor.
