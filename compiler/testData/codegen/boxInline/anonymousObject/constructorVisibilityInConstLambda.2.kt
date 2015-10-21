package test

internal class A {
    inline fun doSomething(): String  {
        return {
            "OK"
        }()
    }
}