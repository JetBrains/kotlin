package a.b

trait Test {
    fun invoke(): String {
        return "OK"
    }
}

private val a : Test = {
    object : Test {

    }
}()

fun box(): String {
    return a.invoke();
}