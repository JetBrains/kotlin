// FILE: main.kt
fun main(args: Array<String>) {
    com.github.salomonbrys.kmffkn.App(testQualified = true)
}

// FILE: app.kt

// Taken from:
// https://github.com/SalomonBrys/kmffkn/blob/master/shared/main/kotlin/com/github/salomonbrys/kmffkn/app.kt

package com.github.salomonbrys.kmffkn

@DslMarker
annotation class MyDsl

@MyDsl
class DslMain {
    fun <T: Any> kClass(block: KClassDsl.() -> T): T = KClassDsl().block()
}

@MyDsl
class KClassDsl {
    inline fun <reified T: Any> of() = T::class
}

fun <T: Any> dsl(block: DslMain.() -> T): T = DslMain().block()

class Test

class App(testQualified: Boolean) {

    @Volatile // This could be noop in Kotlin Native, or the equivalent of volatile in C.
    var type = dsl {
        kClass {
            //kClass {  } // This should error if uncommented because of `@DslMarker`.
            of<Test>()
        }
    }

    init {
        assert(type.simpleName == "Test")
        if (testQualified)
            assert(type.qualifiedName == "com.github.salomonbrys.kmffkn.Test") // This is not really necessary, but always better :).

        assert(String::class == String::class)
        assert(String::class != Int::class)

        assert(Test()::class == Test()::class)
        assert(Test()::class == Test::class)

        println("OK :D")
    }
}
