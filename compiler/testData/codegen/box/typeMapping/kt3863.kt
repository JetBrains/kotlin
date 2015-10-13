import kotlin.reflect.KProperty

// java.lang.VerifyError: (class: NotImplemented, method: get signature: (Ljava/lang/Object;Lkotlin/reflect/KProperty;)Ljava/lang/Object;) Unable to pop operand off an empty stack

class NotImplemented<T>(){
    fun getValue(thisRef: Any?, prop: KProperty<*>): T = notImplemented()
    fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) = notImplemented()
}

fun notImplemented() : Nothing = notImplemented()

class Test {
    val x: Int by NotImplemented<Int>()
}

fun box(): String {
    Test()
    return "OK"
}
