// A
class A {
    fun foo(p1: P?, p2: P?) {}
    fun foo(listener: suspend RS.(P?, P?) -> Unit) {}
}

interface P
interface RS