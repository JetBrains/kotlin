package continuation
// ATTACH_LIBRARY: maven(org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.3.8)-javaagent

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

fun main() {
    val mainTestVal = ""
    runBlocking {
        val rootCoroutineVal = mainTestVal
        TestSuspendA().a()
        print("")
    }
}

class TestSuspendA {
    val classField = ""
    suspend fun a() {
        val methodVal = ""
        InClassB().b()
        print("")
    }

    class InClassB {
        val inClassBField = ""

        suspend fun b() {
            val inClassBMethodVal = ""
            InClassC().c()
            print("")
        }

        inner class InClassC {
            val inClassCField = inClassBField

            suspend fun c() {
                yield()
                val inClassCMethodVal = ""
                //Breakpoint!
                print("")
            }
        }
    }
}