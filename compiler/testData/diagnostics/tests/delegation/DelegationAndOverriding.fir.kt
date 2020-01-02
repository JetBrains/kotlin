package override

interface T {
    fun foo()
    val v : Int
}

open class Br(t : T) : T {

}

class Br3(t : T) : Br(t) {

}

open class Br1(t : T) : T by t {

}

class Br2(t : T) : Br1(t) {

}

interface G<T> {
    fun foo(t : T) : T
}

class GC() : G<Int> {

}

open class GC1(g : G<Int>) : G<Int> by g {

}

open class GC2(g : G<Int>) : GC1(g) {

}