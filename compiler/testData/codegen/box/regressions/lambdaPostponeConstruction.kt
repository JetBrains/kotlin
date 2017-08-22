class MyList<T>

operator fun <T> MyList<T>.plusAssign(element: T) {}

val listOfFunctions = MyList<(Int) -> Int>()

fun foo() {
    listOfFunctions.plusAssign({ it -> it })
    listOfFunctions += { it -> it }
}

fun box(): String {
    return "OK"
}