import kotlin.reflect.KProperty

class FreezableVar<T>(private var value: T)  {
    operator fun getValue(thisRef: Any, property: KProperty<*>): T  = value

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }
}

class LocalFreezableVar<T>(private var value: T)  {
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): T  = value

    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

class Test {
    var x: Boolean by FreezableVar(true)
    var y by FreezableVar("")
}

fun test() {
    var x: Boolean by LocalFreezableVar(true)
    var y by LocalFreezableVar("")
}