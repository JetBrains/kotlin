inline fun test(p: String = "OK"): String {
    return p
}

fun box() : String {
    return test()
}

//mask check in test$default
// 1 IFEQ
// 1 IF