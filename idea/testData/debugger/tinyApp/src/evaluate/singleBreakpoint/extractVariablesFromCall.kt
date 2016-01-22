package extractVariablesFromCall

fun main(args: Array<String>) {
    val a = 1
    val s = "str"
    val klass = MyClass()
    //Breakpoint!
    val c = 0
}

fun f1(i: Int, s: String) = i + s.length
infix fun Int.f2(s: String) = this + s.length

class MyClass {
    fun f1(i: Int, s: String) = i + s.length
}

// EXPRESSION: f1(a, s)
// RESULT: 4: I

// EXPRESSION: a.f2(s)
// RESULT: 4: I

// EXPRESSION: a f2 s
// RESULT: 4: I

// EXPRESSION: klass.f1(a, s)
// RESULT: 4: I
