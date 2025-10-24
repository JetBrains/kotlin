// TARGET_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-21092, KT-81931

class A<T>(val b: T)

fun box(): String {
    val test = A(1).b::javaClass.get().simpleName
    if (test != "Integer") throw Exception("test: $test")
    val test1 = 1::javaClass.get().simpleName
    if (test1 != "Integer") throw Exception("test1: $test1")
    val test2 = Int::javaClass.get(2).simpleName
    if (test2 != "Integer") throw Exception("test2: $test2")

    return "OK"
}

/*
 The correct behaviour would be:
    if (test != "int") throw Exception("test: $test")
    val test1 = A(1 as java.lang.Integer).b::javaClass.get().simpleName
    if (test1 != "Integer") throw Exception("test: $test1")
    val test2 = A(1 as Number).b::javaClass.get().simpleName
    if (test2 != "Integer") throw Exception("test: $test2")
    val test3 = 1::javaClass.get().simpleName
    if (test3 != "int") throw Exception("test: $test3")
    val test4 = (1 as java.lang.Integer)::javaClass.get().simpleName
    if (test4 != "Integer") throw Exception("test: $test4")
    val test5 = (1 as Number)::javaClass.get().simpleName
    if (test5 != "Integer") throw Exception("test: $test5")
 */