open class NoC
class NoC1 : <!SUPERTYPE_NOT_INITIALIZED!>NoC<!>

class WithC0() : NoC()
open class WithC1() : <!SUPERTYPE_NOT_INITIALIZED!>NoC<!>
class NoC2 : <!SUPERTYPE_NOT_INITIALIZED!>WithC1<!>
class NoC3 : WithC1()
class WithC2() : <!SUPERTYPE_NOT_INITIALIZED!>WithC1<!>

class WithPC0() {
}

class WithPC1(<!UNUSED_PARAMETER!>a<!> : Int) {
}


class Foo() : <!FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>WithPC0<!>, <!SYNTAX!>this<!>() {

}

class WithCPI_Dup(<!UNUSED_PARAMETER!>x<!> : Int) {
  <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var x : Int<!>
}

class WithCPI(x : Int) {
  val a = 1
  val xy : Int = x
}

class NoCPI {
  val a = 1
  var ab = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
    get() = 1
    set(<!UNUSED_PARAMETER!>v<!>) {}
}