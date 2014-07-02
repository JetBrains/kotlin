import kotlin.reflect.KMemberProperty

open class Base {
    val foo: Int = 42
}

open class Derived : Base()

fun test() {
    val o = Base::foo
    o : KMemberProperty<Base, Int>
    o.get(Derived()) : Int
}
