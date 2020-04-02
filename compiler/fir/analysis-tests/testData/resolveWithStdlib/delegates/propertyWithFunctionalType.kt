// ISSUE: KT-37304

import kotlin.properties.ReadWriteProperty

interface B

class A {
    private fun <T> property(initialValue: T): ReadWriteProperty<A, T> = null!!

    var conventer by property<(B) -> B>({ it })
    var conventerWithExpectedType: (B) -> B by property({ it })
}