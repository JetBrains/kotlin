// !DIAGNOSTICS: -UNUSED_PARAMETER
annotation class Ann(val x: Int = 1)
class A private (val x: Int) {
inner class B @Ann(2) (val y: Int)

fun foo() {
    class C private @Ann(3) (args: Int)
    }
}
