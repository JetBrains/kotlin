// IGNORE_BACKEND_FIR: JVM_IR
class A() {
  fun action() = "OK"

  infix fun infix(a: String) = "O" + a

  val property = "OK"

  val a : A
    get() = A()
}

fun test1()  = A()!!.property
fun test2()  = (A() as A?)!!.property
fun test3()  = A()!!.action()
fun test4()  = (A() as A?)!!.action()
fun test5()  = (null as A?)!!.action()
fun test6()  = A().a.a!!.action()
fun test7()  = 10!!.plus(11)
fun test8()  = (10 as Int?)!!.plus(11)
fun test9()  = A()!! infix "K"
fun test10() = (A() as A?) !! infix "K"
fun test11() = (A() as A?) !! infix("K")
fun test12()  = A()!! infix ("K")

fun box() : String {
  if(test1() != "OK") return "fail"
  if(test2() != "OK") return "fail"
  if(test3() != "OK") return "fail"
  if(test4() != "OK") return "fail"

  try {
      test5()
      return "fail"
  }
  catch(e: NullPointerException) { //
  }

  if(test6() != "OK") return "fail"
  if(test7() != 21) return "fail"
  if(test8() != 21) return "fail"

  if(test9() != "OK") return "fail"
  if(test10() != "OK") return "fail"
  if(test11() != "OK") return "fail"
  if(test12() != "OK") return "fail"

  return "OK"
}
