package override

interface T {
    fun foo()
    val v : Int
}

open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Br<!>(<!UNUSED_PARAMETER!>t<!> : T) : T {

}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class Br3<!>(t : T) : Br(t) {

}

open class Br1(t : T) : T by t {

}

class Br2(t : T) : Br1(t) {

}

interface G<T> {
    fun foo(t : T) : T
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class GC<!>() : G<Int> {

}

open class GC1(g : G<Int>) : G<Int> by g {

}

open class GC2(g : G<Int>) : GC1(g) {

}