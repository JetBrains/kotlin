// EXPECTED_DUPLICATED_HIGHLIGHTING
open class NoC
class NoC1 : NoC()

class WithC0() : NoC()
open class WithC1() : NoC()
class NoC2 : <error descr="[SUPERTYPE_NOT_INITIALIZED] This type has a constructor, and thus must be initialized here">WithC1</error>
class NoC3 : WithC1()
class WithC2() : <error descr="[SUPERTYPE_NOT_INITIALIZED] This type has a constructor, and thus must be initialized here">WithC1</error>

class NoPC {
}

class WithPC0() {
}

class WithPC1(a : Int) {
}


class Foo() : <error descr="[FINAL_SUPERTYPE] This type is final, so it cannot be inherited from">WithPC0</error>(), <error descr="Type expected"><error descr="[SYNTAX] Syntax error"><error descr="[SYNTAX] Syntax error">this</error></error></error>() {

}

class WithCPI_Dup(x : Int) {
  <error descr="[MUST_BE_INITIALIZED_OR_BE_ABSTRACT] Property must be initialized or be abstract">var x : Int</error>
}

class WithCPI(x : Int) {
  val a = 1
  val xy : Int = x
}

class NoCPI {
  val a = 1
  var ab = <error descr="[PROPERTY_INITIALIZER_NO_BACKING_FIELD] Initializer is not allowed here because this property has no backing field">1</error>
    get() = 1
    set(v) {}
}
