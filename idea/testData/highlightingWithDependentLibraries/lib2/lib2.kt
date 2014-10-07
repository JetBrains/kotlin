package lib2

import lib1.*

public class Derived(): Base() {
    public fun derivedFun() {
    }
}

public fun acceptBase(b: Base) {
}

public fun returnBase(): Base = Base()

public fun Base.extendBase(): Unit {

}