// IGNORE_BACKEND_FIR: JVM_IR
package t

class Reproduce {

    fun test(): String {
        data class Foo(val bar: String, val baz: Int)
        val foo = Foo("OK", 5)
        return foo.bar
    }
}

fun box() : String {
    return Reproduce().test()
}