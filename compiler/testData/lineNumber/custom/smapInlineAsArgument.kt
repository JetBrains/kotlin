fun box(){
    checkEquals(test(),
                fail())

    checkEquals(fail(),
                test())
}

public fun checkEquals(p1: String, p2: String) {
    throw AssertionError("fail")
}

inline fun test() : String {
    return "123"
}

fun fail() : String {
    throw AssertionError("fail")
}

// 2 22 3 2 5 6 23 5 7 10 14 18