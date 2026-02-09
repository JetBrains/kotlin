// ISSUE: KT-84167
// RUN_PIPELINE_TILL: FRONTEND

// FILE: part1/part2/part3/test.kt

package part1.part2.part3

class C {
    fun M() { }
}

fun test() {
    val c: part1.part2<Int>.part3.C = C()
    if (c is part1<Int>.part2.part3.C) {
        c.M()
    }
}
