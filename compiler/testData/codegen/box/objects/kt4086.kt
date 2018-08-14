interface N

open class Base(n: N)

class Derived : Base(object: N{}) {

}

fun box() : String {
    Derived()
    return "OK"
}