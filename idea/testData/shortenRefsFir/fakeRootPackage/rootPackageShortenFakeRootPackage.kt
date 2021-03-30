// FIR_COMPARISON
package test

class Test

val property: Int = 10

fun test() {}

fun usage() {
    class Test

    val property: String = ""

    fun test() {}

    <selection>
    _root_ide_package_.test.property
    _root_ide_package_.test.test()
    _root_ide_package_.test.Test
    val t: _root_ide_package_.test.Test
    </selection>
}