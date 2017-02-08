fun foo1(arg: String) : String = arg

/*
fun foo2(arg: String) : String = arg + " foo2"

fun bar(arg1: String, arg2: String) : String  = arg1 + " bar " + arg2

fun zoo1() : String {
    var x = foo1("")
    var y = 4
    return x
}

fun zoo2() : String {
    val x = foo1("")
    var y = 5
    return x
}

class Node(var previous: Node?)

fun zoo3() : Node {
    var current = Node(null)
    for (i in 1 .. 5) {
        current = Node(current)
    }
    return current
}

fun zoo4(arg: Int) : Any {
    var a = Any()
    var b = Any()
    var c = Any()
    a = b
    val x = 3
    a = when {
        x < arg -> b
        else -> c
    }
    return a
} */

fun zoo5(arg: String) : String {
    return arg + foo1(arg)
}

fun zoo6(arg: Any) : Any {
    return zoo7(arg, "foo", 11)
}

fun zoo7(arg1: Any, arg2: Any, selector: Int) : Any {
    return if (selector < 2) arg1 else arg2;
}

fun main(args : Array<String>) {
    val z = zoo7(Any(), Any(), 1)
    //println(bar(foo1(foo2("")), foo2(foo1(""))))
}
