// EXPECTED_DUPLICATED_HIGHLIGHTING
open class NoC
class NoC1 : NoC()

class WithC0() : NoC()
open class WithC1() : NoC()
class NoC2 : <error>WithC1</error>
class NoC3 : WithC1()
class WithC2() : <error>WithC1</error>

class NoPC {
}

class WithPC0() {
}

class WithPC1(<warning>a</warning> : Int) {
}


class Foo() : <error>WithPC0</error>(), <error>this</error>() {

}

class WithCPI_Dup(<warning>x</warning> : Int) {
  <error>var x : Int</error>
}

class WithCPI(x : Int) {
  val a = 1
  val xy : Int = x
}

class NoCPI {
  val a = 1
  var ab = <error>1</error>
    get() = 1
    set(<warning>v</warning>) {}
}