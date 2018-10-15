// IGNORE_BACKEND: JVM_IR
open class Bar<T>(val prop: String)
object Foo : Bar<Foo>("OK") {

    val p = Foo.prop
    val p2 = prop
    val p3 = this.prop
}
fun box(): String = Foo.prop