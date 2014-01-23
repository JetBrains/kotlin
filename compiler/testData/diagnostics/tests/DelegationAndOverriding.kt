package override

trait T {
    fun foo()
    val v : Int
}

open class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>Br<!>(t : T) : T {

}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>Br3<!>(t : T) : Br(t) {

}

open class Br1(t : T) : T by t {

}

class Br2(t : T) : Br1(t) {

}

trait G<T> {
    fun foo(t : T) : T
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>GC<!>() : G<Int> {

}

open class GC1(g : G<Int>) : G<Int> by g {

}

open class GC2(g : G<Int>) : GC1(g) {

}
