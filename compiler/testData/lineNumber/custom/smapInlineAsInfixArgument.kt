infix fun String.execute(p: String) = this + p

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

// 1 4 20 5 4 7 8 21 7 9 12 16