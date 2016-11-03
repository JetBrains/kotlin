// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class A() {
    fun foo() {
        System.out?.println(1)
    }
}

fun box() : String {
    val a : A = A()
    return "OK"
}
