package codegen.kclass.kclass1

import kotlin.test.*

// FILE: main.kt
@Test fun runTest() {
    App(testQualified = true)
}

// FILE: app.kt

// Taken from:
// https://github.com/SalomonBrys/kmffkn/blob/master/shared/main/kotlin/com/github/salomonbrys/kmffkn/app.kt

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

class TestClass

class App(testQualified: Boolean) {

    @Volatile // This could be noop in Kotlin Native, or the equivalent of volatile in C.
    var type = dsl {
        kClass {
            //kClass {  } // This should error if uncommented because of `@DslMarker`.
            of<TestClass>()
        }
    }

    init {
        assert(type.simpleName == "TestClass")
        if (testQualified)
            assert(type.qualifiedName == "codegen.kclass.kclass1.TestClass") // This is not really necessary, but always better :).

        assert(String::class == String::class)
        assert(String::class != Int::class)

        assert(TestClass()::class == TestClass()::class)
        assert(TestClass()::class == TestClass::class)

        println("OK :D")
    }
}
