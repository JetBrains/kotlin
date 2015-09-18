// KT-575 Cannot ++ a companion object member

package kt575

class Creature() {
    companion object {
        var numCreated : Int = 0
          private set
    }

    object A {
        var bbb : Int

        init {
            <!BACKING_FIELD_USAGE_DEPRECATED!>$bbb<!> = 1
        }
    }

    class C() {
        var ccc : Int

        init {
            <!BACKING_FIELD_USAGE_DEPRECATED!>$ccc<!> = 2
        }
    }

    init {
        Creature.numCreated++ // Error
        A.bbb++
        C().ccc++
    }
}
