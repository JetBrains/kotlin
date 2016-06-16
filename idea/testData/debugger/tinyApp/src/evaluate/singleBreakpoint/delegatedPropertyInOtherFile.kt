package delegatedPropertyInOtherFile

import delegatedPropertyInOtherFileOther.*

fun main(a: Array<String>) {
    val t = WithDelegate()

    //Breakpoint!
    t.a
}

// EXPRESSION: t.a
// RESULT: 12: I
