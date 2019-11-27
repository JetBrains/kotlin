// IGNORE_BACKEND_FIR: JVM_IR
package testing

class Test {
    private val hello: String
        get() { return "hello" }

    fun sayHello() : String = hello
}

fun box(): String {
  return if (Test().sayHello() == "hello") "OK" else "fail"
}