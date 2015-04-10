
open class Base() {
    var v : Int = 0
}
    
open class Left() : Base()

trait Right : <!TRAIT_WITH_SUPERCLASS!>Base<!>

class Diamond() : Left(), Right
