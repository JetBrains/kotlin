open class NoC
class NoC1 : NoC

class WithC0() : NoC()
open class WithC1() : NoC
class NoC2 : WithC1
class NoC3 : WithC1()
class WithC2() : WithC1

class WithPC0() {
}

class WithPC1(a : Int) {
}


class Foo() : WithPC0, <!SYNTAX!>this<!>() {

}

class WithCPI_Dup(x : Int) {
  var x : Int
}

class WithCPI(x : Int) {
  val a = 1
  val xy : Int = x
}

class NoCPI {
  val a = 1
  var ab = 1
    get() = 1
    set(v) {}
}