// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 2 -> sentence 3
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 3
 * overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 3 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Explicitly imported extension callables for for-loop operator iterator call
 */

// FILE: LibMyIterator.kt
package libMyIteratorPackage

var flag = false

class MyIterator(shouldBeCalled: Boolean = false) : CharIterator() {
    init {
        flag = shouldBeCalled
    }
    private var index = 0
    override fun nextChar(): Char { index++;  return 'd'}
    override fun hasNext(): Boolean = index < 5
}

// FILE: Lib1.kt
package libPackage1
import libMyIteratorPackage.*
import testPack.Iterable
import testPack.Inv

operator fun Iterable.iterator() : CharIterator = MyIterator(true)
operator fun Inv.invoke() : CharIterator = MyIterator()

// FILE: Lib2.kt
package libPackage2
import libMyIteratorPackage.*
import testPack.Iterable
import testPack.Inv

operator fun Iterable.iterator() : CharIterator = MyIterator()
operator fun Inv.invoke() : CharIterator = MyIterator()

// FILE: Test.kt
package testPack
import libMyIteratorPackage.*
import libPackage1.iterator
import libPackage1.invoke
import libPackage2.*


class Iterable(iterator: Inv) {
    /*operator fun iterator(): CharIterator = MyIterator()*/
}

class Inv(val c: Char) {
    operator fun invoke(): CharIterator = MyIterator()
}

operator fun Iterable.iterator(): CharIterator = MyIterator()

fun box(): String {
    /*operator fun Iterable.iterator(): CharIterator = MyIterator()*/

    for (i in Iterable(Inv('c'))) {
        println(i)
    }
    return if (flag)
        "OK"
    else "NOK"
}

