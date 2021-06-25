open class Foo() {

}

class Barrr() : <!DELEGATION_NOT_TO_INTERFACE, SUPERTYPE_NOT_INITIALIZED!>Foo<!> by Foo() {}

interface T {}

class Br(t : T) : T by t {}

open enum class EN() {
  A
}

class Test2(e : EN) : <!DELEGATION_NOT_TO_INTERFACE!>EN<!> by e {}
