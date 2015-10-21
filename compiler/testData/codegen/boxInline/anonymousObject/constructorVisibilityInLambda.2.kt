package test

internal class A {
    inline fun doSomething(s: String): String  {
        return {
            s
        }()
    }
}