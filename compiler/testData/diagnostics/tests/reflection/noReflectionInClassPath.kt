import kotlin.reflect.*

class Foo(val prop: Any) {
    fun func() {}
}

fun n01() = Foo::prop
fun n02() = Foo::func
fun n03() = Foo::class
fun n04(p: KProperty0<Int>) = p.get()
fun n05(p: KMutableProperty0<String>) = p.set("")
fun n06(p: KProperty0<Int>) = p.get()
fun n07(p: KFunction<String>) = p.name
fun n08(p: KProperty1<String, Int>) = p.get("")
fun n09(p: KProperty2<String, String, Int>) = p.get("", "")
fun n10() = (Foo::func).invoke(Foo(""))
fun n11() = (Foo::func)(Foo(""))

fun y01() = Foo::prop.<!NO_REFLECTION_IN_CLASS_PATH!>getter<!>
fun y02() = Foo::class.<!NO_REFLECTION_IN_CLASS_PATH!>members<!>
fun y03() = Foo::class.<!NO_REFLECTION_IN_CLASS_PATH!>simpleName<!>
fun y04() = Foo::class.<!UNRESOLVED_REFERENCE!>properties<!>
