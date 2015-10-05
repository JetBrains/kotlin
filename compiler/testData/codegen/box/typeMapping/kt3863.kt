// java.lang.VerifyError: (class: NotImplemented, method: get signature: (Ljava/lang/Object;Ljet/PropertyMetadata;)Ljava/lang/Object;) Unable to pop operand off an empty stack

class NotImplemented<T>(){
    fun getValue(thisRef: Any?, prop: PropertyMetadata): T = notImplemented()
    fun setValue(thisRef: Any?, prop: PropertyMetadata, value: T) = notImplemented()
}

fun notImplemented() : Nothing = notImplemented()

class Test {
    val x: Int by NotImplemented<Int>()
}

fun box(): String {
    Test()
    return "OK"
}
