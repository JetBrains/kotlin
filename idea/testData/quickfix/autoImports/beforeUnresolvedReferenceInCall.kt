// "Import" "true"
// ERROR: Please specify constructor invocation; classifier 'ArrayList' does not have a companion object

// KT-4000

package testing

class Test {
    fun foo(a: Collection<String>) {

    }
}

fun test() {
    val t = Test()
    t.foo(<caret>ArrayList)
}