//KT-1944 Inference fails on run()
package j

class P {
    var x : Int = 0
        private set

    fun foo() {
        val r = run {x = 5} // ERROR
        r : Unit
    }
}

fun <T> run(f: () -> T) : T = f()
