package bbb

trait One {
  public open fun foo() : Int
  public open fun faz() : Int = 10
}
trait Two {
  public open fun foo() : Int
  public open fun quux() : Int = 100
}

class OneImpl : One {
  public override fun foo() = 1
}
class TwoImpl : Two {
  public override fun foo() = 2
}

class Test2(a : One, b : Two) : Two by b, One by a {
  public override fun foo() = 0
}

class TestJava(r : Runnable) : Runnable by r {}
class TestRunnable() : Runnable {
  public override fun run() = System.out.println("foobar")
}

fun box() : String {
    var del = TestJava(TestRunnable())
    del.run()
    if (del !is Runnable)
        return "Fail #1"

    var t2 = Test2(OneImpl(), TwoImpl())
    if (t2.foo() != 0)
        return "Fail #2"
    if (t2.faz() != 10)
        return "Fail #3"
    if (t2.quux() != 100)
        return "Fail #4"
    if (t2 !is One)
        return "Fail #5"
    if (t2 !is Two)
        return "Fail #6"

    return "OK"
}
