// RUN_PIPELINE_TILL: CODEGEN

open class Class {
    fun Int.test() {}
    val Int.test
        get() = 0
}

class MyClass1 : Class()
