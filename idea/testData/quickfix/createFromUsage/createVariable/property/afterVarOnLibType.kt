// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized

class A<T>(val n: T)

fun test() {
    2.foo = A("2")
}

var Int.foo: A<String>
