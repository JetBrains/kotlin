// KT-61929
// WITH_SDTLIB
// IGNORE_BACKEND: JVM
// EXPECTED_REACHABLE_NODES: 1301
package foo

fun doSomething(lambda: () -> Unit) { lambda() }

class CompilerBug1(result: String) {
    var result: String = "Failed"

    init {
        run {
            object {
                init {
                    doSomething { completed(result) }
                }
            }
        }
    }

    fun completed(value: String) {
        this.result = value
    }
}

class CompilerBug2(result: String) {
    var result: String = "Fail"

    init {
        run {
            class Foo {
                init {
                    doSomething { completed(result) }
                }

                constructor() {}
            }

            Foo()
        }
    }

    fun completed(value: String) {
        this.result = value
    }
}

class CompilerBug3(result: String) {
    var result: String = "OK"

    init {
        run {
            class Foo {
                init {
                    doSomething { completed(result) }
                }

                constructor() {}
                constructor(test: String) {}
            }

            if (this.result == "OK") {
                this.result = "Failed with the empty constructor"
                Foo()
            }
            if (this.result == "OK")  {
                this.result = "Failed with one parameter constructor"
                Foo("Test")
            }
        }
    }

    fun completed(value: String) {
        this.result = value
    }
}

fun box(): String {
    CompilerBug1("OK").result.also { if (it != "OK") return "CompilerBug1: $it" }
    CompilerBug2("OK").result.also { if (it != "OK") return "CompilerBug2: $it" }
    CompilerBug3("OK").result.also { if (it != "OK") return "CompilerBug3: $it" }

    return "OK"
}