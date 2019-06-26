// FILE: delegatedPropertyInOtherFile.kt
package delegatedPropertyInOtherFile

import delegatedPropertyInOtherFileOther.*

fun main(a: Array<String>) {
    val t = WithDelegate()

    //Breakpoint!
    t.a
}

// EXPRESSION: t.a
// RESULT: 12: I

// FILE: delegatedPropertyInOtherFile/delegatedPropertyInOtherFile2.kt
package delegatedPropertyInOtherFileOther

import kotlin.reflect.KProperty

class WithDelegate {
    val a: Int by Id(12)
}

class Id(val v: Int) {
    operator fun getValue(o: Any, property: KProperty<*>): Int = v
}
