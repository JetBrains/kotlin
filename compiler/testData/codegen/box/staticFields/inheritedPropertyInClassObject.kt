open class Bar<T>(val prop: String)
class Foo {
    default object : Bar<Foo>("OK") {
        val p = Foo.prop
        val p2 = prop
        val p3 = this.prop
    }

    val p4 = Foo.prop
    val p5 = prop
}

fun box(): String = Foo.prop