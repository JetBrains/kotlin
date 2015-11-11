// "Import" "true"
// ERROR: Function invocation 'ArrayList()' expected

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