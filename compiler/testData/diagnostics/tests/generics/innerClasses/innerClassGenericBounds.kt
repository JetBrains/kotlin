// ISSUE: KT-61068, KT-35566
// FILE: generic.kt
package generic

interface Foo

open class SuperOuter<T> {
    open inner class SuperInner<D : Foo>(database: D)
}

class SubOuter : SuperOuter<Unit>() {
    inner class SubInner(database: Any?) : SuperInner<Any?>(database)
}

// FILE: nongeneric.kt
package nongeneric

interface Foo

open class SuperOuter {
    open inner class SuperInner<D : Foo>(database: D)
}

class SubOuter : SuperOuter() {
    inner class SubInner(database: Any?) : SuperInner<<!UPPER_BOUND_VIOLATED!>Any?<!>>(database)
}

// FILE: kt35566.kt
package kt35566

open class Case1<K : Number> {
    open inner class Case1_1<L> : Case1<Int>() where L : CharSequence {
        var x: L? = null

        inner class Case1_2<M>(m: M) : Case1<K>.Case1_1<M>() where M : Map<K, L> {
            init {
                x = m
            }
        }
    }
}
