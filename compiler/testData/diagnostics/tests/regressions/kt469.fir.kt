
package kt469

//KT-512 plusAssign() : Unit does not work properly
import java.util.*

fun bar(list : MutableList<Int>) {
    for (i in 1..10) {
        list += i // error
    }
    System.out.println(list)
}

operator fun <T> MutableList<T>.plusAssign(t : T) {
    add(t)
}

//KT-469 Allow val-reassignment when appropriate functions are defined
fun foo() {
    val m = MyNumber(2)
    m -= MyNumber(3)           //should not be error here
}

class MyNumber(var i: Int) {
    operator fun minusAssign(m : MyNumber) {
        i -= m.i
    }
}
