class UsedClassTypeParameter<T>(t: T) {
    {
        println(t)
    }
}

class UsedClassTypeParameter2<T> {
    fun test() {
        Foo(this)
    }

    private class Foo<U>(foo: UsedClassTypeParameter2<U>) {
        init {
            println(foo)
        }
    }
}

class UsedClassTypeParameter3<T>

fun <U> foo3(foo: UsedClassTypeParameter3<U>) {
    println(foo)
}

fun main(args: Array<String>) {
    UsedClassTypeParameter("")
    UsedClassTypeParameter2<Int>().test()
    foo3(UsedClassTypeParameter3<Int>())
}