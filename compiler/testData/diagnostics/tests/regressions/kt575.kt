// KT-575 Cannot ++ a class object member

package kt575

class Creature() {
    class object {
        var numCreated : Int = 0
          private set
    }

    object A {
        var bbb : Int

        {
            $bbb = 1
        }
    }

    class C() {
        var ccc : Int

        {
            $ccc = 2
        }
    }

    {
        Creature.numCreated++ // Error
        A.bbb++
        C().ccc++
    }
}
