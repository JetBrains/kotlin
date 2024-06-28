// RESOLVE_SCRIPT

class UnusedClass {
    var version: String = ""

    fun execute() {
        println(version)
    }
}

fun unusedFunction() = 4
fun build(action: () -> Unit) {}
var unusedVariable = 4
var variable = "str"

build {
    class A {
        fun doo(i: Int) {

        }
    }
}

variable = "1"