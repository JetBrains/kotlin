open class Foo() {

}

class Barrr() : <!DELEGATION_NOT_TO_TRAIT!>Foo<!> by Foo() {}

trait T {}

class Br(t : T) : T by t {}

open enum class E() {}

class Test2(e : E) : <!DELEGATION_NOT_TO_TRAIT!>E<!> by e {}
