// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// Enable for JS when it supports Java class library.

class TestJava(r : Runnable) : Runnable by r {}
class TestRunnable() : Runnable {
  public override fun run() = System.out.println("foobar")
}

fun box() : String {
    var del = TestJava(TestRunnable())
    del.run()
    if (del !is Runnable)
        return "Fail #1"

    return "OK"
}
