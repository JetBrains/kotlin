package dataClassCopy

fun main() {
    val a = Foo("foo")
    //Breakpoint!
    a.copy("bar")
}

data class Foo(val a: String)

// STEP_INTO: 1
// STEP_OUT: 1

// EXPRESSION: Foo("baz")
// RESULT: instance of dataClassCopy.Foo(id=ID): LdataClassCopy/Foo;

// EXPRESSION: a
// RESULT: Unresolved reference: a