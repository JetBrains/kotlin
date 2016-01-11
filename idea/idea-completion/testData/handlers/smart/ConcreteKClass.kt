import kotlin.reflect.KClass

open class A<T : Any>(val kClass: KClass<T>?)

class B : A<java.io.File>(<caret>)

// ELEMENT: File
