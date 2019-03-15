//KT-2514 Type inference fails when using extension function literal
package kt2514

//+JDK
import java.io.Closeable

fun <T> Thread.use(block: Thread.() -> T): T = block()

fun <T: Closeable, R> T.use(block: (T)-> R) : R = block(this)

fun main() {
    Thread().use { }            // compilation error: Type inference failed
    Thread().use { 5 + 5 }      // compilation error: Type inference failed
    Thread().use<Unit> { }      // compiles okay
    Thread().use<Int> { 5 + 5 } // compiles okay
}