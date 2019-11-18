// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty

// java.lang.VerifyError: (class: NotImplemented, method: get signature: (Ljava/lang/Object;Lkotlin/reflect/KProperty;)Ljava/lang/Object;) Unable to pop operand off an empty stack

class NotImplemented<T>(){
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T = notImplemented()
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T): Nothing = notImplemented()
}

fun notImplemented() : Nothing = notImplemented()

class Test {
    val x: Int by NotImplemented<Int>()
}

fun box(): String {
    Test()
    return "OK"
}
