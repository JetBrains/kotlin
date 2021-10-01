open class SuperClass(val arg: () -> String)

object obj {

    fun foo(): String {
        return "OK"
    }

    class Foo : SuperClass(::foo)
}

fun box(): String {
    return obj.Foo().arg()
}
