package inlineFun1

inline fun myFun(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) f: () -> Int): Int {
    val o = object {
        fun test() = 1
    }
    o.test()

    val lambda = { 1 }
    lambda()

    val o2 = object {
        fun test() = f()
    }
    o2.test()

    val lambda2 = { f() }
    lambda2()

    val ref = ::reference
    ref.invoke()

    return f()
}

fun reference() = 1