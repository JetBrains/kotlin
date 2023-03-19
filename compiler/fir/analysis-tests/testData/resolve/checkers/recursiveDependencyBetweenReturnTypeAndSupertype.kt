// ISSUE: KT-47815

interface A : <!INTERFACE_WITH_SUPERCLASS!>Test<!>

open class Test {
    fun <T> result() = object : A { }
}
