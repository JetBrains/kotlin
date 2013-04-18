open class Base {
    val pr : String = "OK"
}

trait Trait : Base {
    fun f() : String {
        return this.pr
    }
}

class A : Trait, Base() {

}

fun box() : String {
    return if (A().f() == A().pr)  "OK" else "fail"
}