// !DIAGNOSTICS: -UNUSED_VARIABLE

//FILE:file1.kt
package a

private open class A {
    fun bar() {}
}

private var x: Int = 10

private fun foo() {}

private fun bar() {
    val y = x
    x = 20
}

fun makeA() = A()

private object PO {}

//FILE:file2.kt
package a

fun test() {
    val y = makeA()
    y.<!ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE!>bar<!>()
    <!ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE!>foo<!>()

    val u : <!ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE!>A<!> = <!ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE!>A<!>()

    val z = <!ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE!>x<!>
    <!ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE!>x<!> = 30

    val po = <!ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE!>PO<!>
}

class B : <!ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE, ACCESS_TO_PRIVATE_TOP_LEVEL_FROM_ANOTHER_FILE!>A<!>() {}

class Q {
    class W {
        fun foo() {
            val y = makeA() //assure that 'makeA' is visible
        }
    }
}