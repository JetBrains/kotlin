fun box(){
    test() +
            fail()

    fail() +
                test()
}

inline fun test() : String {
    return "123"
}

fun fail() : String {
    throw AssertionError("fail")
}

// 2 18 3 5 6 19 7 10 14