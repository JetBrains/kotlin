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

// 2 10 3 5 6 10 7 10 14