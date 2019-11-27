// IGNORE_BACKEND_FIR: JVM_IR
fun Any.foo1() : ()-> String {
  return { "239" + this }
}

fun Int.foo2() : (i : Int) -> Int {
  return { x -> x + this }
}

fun <T> fooT1(t : T) = { t.toString() }

fun <T> fooT2(t: T) = { x:T -> t.toString() + x.toString() }

object t

fun box() : String {
  if( (10.foo1())() != "23910") return "foo1 fail"
  if( (10.foo2())(1) != 11 ) return "foo2 fail"

  if(1.(fun Int.() = this + 1)() != 2) return "test 3 failed";
  if(  {1}() != 1)  return "test 4 failed";
  if(  {x : Int -> x}(1) != 1)  return "test 5 failed";
  if(  1.(fun Int.(x: Int) = x + this)(1) != 2) return "test 6 failed";
  if(  t.(fun Any.() = this)() != t)  return "test 7 failed";
  if(  (fooT1<String>("mama"))() != "mama")  return "test 8 failed";
  if(  (fooT2<String>("mama"))("papa") != "mamapapa")  return "test 9 failed";
  return "OK"
}
