open class Foo() {

}

class Barrr() : <!DELEGATION_NOT_TO_TRAIT!>Foo<!> by Foo() {}

interface T {}

class Br(t : T) : T by t {}

<!OPEN_MODIFIER_IN_ENUM!>open<!> enum class EN() {
  A
}

class Test2(e : EN) : <!DELEGATION_NOT_TO_TRAIT!>EN<!> by e {}
