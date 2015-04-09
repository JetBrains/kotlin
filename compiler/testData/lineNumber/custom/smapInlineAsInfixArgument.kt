fun String.execute(p: String) = this + p

fun box(){
    test() execute
            fail()

    fail() execute
            test()
}

inline fun test() : String {
    return "123"
}

fun fail() : String {
    throw AssertionError("fail")
}

// 1 4 12 4 7 12 7 9 12 16