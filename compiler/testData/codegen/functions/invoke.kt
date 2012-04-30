package invoke

fun test1(predicate: (Int) -> Int, i: Int) = predicate(i)

fun test2(predicate: (Int) -> Int, i: Int) = predicate.invoke(i)

class Method {
    fun invoke(i: Int) = i
}

fun test3(method: Method, i: Int) = method.invoke(i)

//todo
//fun test4(method: Method, i: Int) = method(i)

fun box() : String {
    if (test1({ it }, 1) != 1) return "fail 1"
    if (test2({ it }, 2) != 2) return "fail 2"
    if (test3(Method(), 3) != 3) return "fail 3"
    //if (test4(Method(), 4) != 4) return "fail 4"
    return "OK"
}
