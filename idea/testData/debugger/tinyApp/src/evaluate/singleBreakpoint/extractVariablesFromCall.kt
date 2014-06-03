package extractVariablesFromCall

fun main(args: Array<String>) {
    val a = 1
    val s = "str"
    val klass = MyClass()
    //Breakpoint!
    val c = 0
}

fun f1(i: Int, s: String) = i + s.size
fun Int.f2(s: String) = this + s.size

class MyClass {
    fun f1(i: Int, s: String) = i + s.size
}

// EXPRESSION: f1(a, s)
// RESULT: 4: I

// EXPRESSION: a.f2(s)
// RESULT: 4: I

// EXPRESSION: a f2 s
// RESULT: 4: I

// EXPRESSION: klass.f1(a, s)
// RESULT: 4: I
