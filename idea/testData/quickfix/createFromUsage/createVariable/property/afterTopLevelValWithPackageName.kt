// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized

package foo

fun test(): Int {
    return foo
}

val foo: Int
