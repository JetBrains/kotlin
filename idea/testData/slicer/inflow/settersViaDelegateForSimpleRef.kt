// FLOW: IN
// RUNTIME_WITH_REFLECT

import kotlin.reflect.KProperty

class Delegate {
    private var _value: String = ""
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = _value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        _value = value
    }
}

class AClass(name1: String){
    var name by Delegate()
    init {
        name = name1
    }

    fun uses(){
        name = "bye"
        println("Now my name is '$<caret>name'")
    }
}

fun main(args: Array<String>) {
    val a = AClass("hello")
    println("My name is '${a.name}'")
}