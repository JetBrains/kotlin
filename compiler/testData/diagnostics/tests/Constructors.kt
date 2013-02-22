open class NoC
class NoC1 : <!SUPERTYPE_NOT_INITIALIZED!>NoC<!>

class WithC0() : NoC()
open class WithC1() : <!SUPERTYPE_NOT_INITIALIZED!>NoC<!>
class NoC2 : <!SUPERTYPE_NOT_INITIALIZED!>WithC1<!>
class NoC3 : WithC1()
class WithC2() : <!SUPERTYPE_NOT_INITIALIZED!>WithC1<!>

class WithPC0() {
}

class WithPC1(a : Int) {
}


class Foo() : <!SUPERTYPE_NOT_INITIALIZED, FINAL_SUPERTYPE!>WithPC0<!>, <!SYNTAX!>this<!>() {

}

class WithCPI_Dup(x : Int) {
  <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var x : Int<!>
}

class WithCPI(x : Int) {
  val a = 1
  val b : Int = $a
  val xy : Int = x
}

class NoCPI {
  val a = 1
  var ab = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
    get() = 1
    set(v) {}
}