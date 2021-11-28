package a.b

fun <T> eval(fn: () -> T) = fn()

interface Test {
    fun invoke(): String {
        return "OK"
    }
}

private val a : Test = eval {
    object : Test {}
}

fun box(): String {
    return a.invoke();
}