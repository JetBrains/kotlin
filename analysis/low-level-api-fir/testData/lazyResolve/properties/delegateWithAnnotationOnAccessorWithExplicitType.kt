import kotlin.reflect.KProperty

@get:Anno("getter")
@set:Anno("setter")
@setparam:Anno("parameter")
@delegate:Anno("delegate")
var f<caret>oo: Int by MyDelegate()

@Repeatable
annotation class Anno(val s: String)

class MyDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = 42
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
}