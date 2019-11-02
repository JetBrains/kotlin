interface A<T> {
    abstract  fun foo() : T
}
interface B<T>, A<T> {
}
class BImpl<T>(val a: A<T>): B<T>, A<T> by a {
}
fun box() : String  {
    val b : B<String> = BImpl<String>< String >(object: A<String> {
    override fun foo() : String  {
        return "OK"
    }

})
    if (b.foo() != "OK") {
        return "fail 1"
    }
    val a : A<String> = b
    return a.foo()
}
