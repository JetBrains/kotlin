package invoke

fun test1(predicate: (Int) -> Int, i: Int) = predicate(i)

fun test2(predicate: (Int) -> Int, i: Int) = predicate.invoke(i)

class Method {
    operator fun invoke(i: Int) = i
}

fun test3(method: Method, i: Int) = method.invoke(i)

fun test4(method: Method, i: Int) = method(i)

class Method2 {}

operator fun Method2.invoke(s: String) = s

fun test5(method2: Method2, s: String) = method2(s)

fun box() : String {
    if (test1({ it }, 1) != 1) return "fail 1"
    if (test2({ it }, 2) != 2) return "fail 2"
    if (test3(Method(), 3) != 3) return "fail 3"
    if (test4(Method(), 4) != 4) return "fail 4"
    if (test5(Method2(), "s") != "s") return "fail 5"
    if (test1(Int::dec, 1) != 0) return "fail 6"
    if (test2(Int::dec, 1) != 0) return "fail 7"
    if (test1(fun(x: Int) = x - 1, 1) != 0) return "fail 8"
    return "OK"
}
