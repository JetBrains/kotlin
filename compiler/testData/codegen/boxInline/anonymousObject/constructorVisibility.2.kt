package test

interface Run {
    fun run(): String
}

internal class A {
    inline fun doSomething(): Run  {
        return object : Run {
            override fun run(): String =  "OK"
        }
    }
}