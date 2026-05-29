package pack

fun overloaded(x: Int) {}
fun overloaded(x: String) {}

class Holder {
    fun method(x: Int) {}
    fun method(x: String) {}

    val ambiguousMember: Int = 0
    fun ambiguousMember() {}
}

fun simple(x: Int) {}
val simpleProperty: Int = 0

class Single {
    fun method(x: Int) {}
    val property: Int = 0
}

fun <T> generic(x: T): T = x

class GenericHolder<T> {
    fun <U> generic(x: U): T = TODO()
    val property: T get() = TODO()
}

class LoggerContext

context(c: LoggerContext)
fun withContext(x: Int) {}

context(c: LoggerContext)
val withContextProperty: Int get() = 0

fun ambiguousFunctionReference() {
    ::overloaded
}

fun ambiguousMemberMethodReference() {
    Holder::method
    Holder()::method
}

fun ambiguousFunctionAndPropertyReference() {
    Holder::ambiguousMember
}

fun callableReferenceWithArguments() {
    ::overloaded(true)
    Holder::method(Holder(), true)
}

fun unambiguousCallableReferenceWithArguments() {
    ::simple(1)
    ::simpleProperty
    Single::method(Single(), 1)
    Single()::method(1)
    Single::property(Single())
    Single()::property
}

fun callableReferenceWithTypeArguments() {
    ::generic<Int>
    ::generic<Int>(1)
    GenericHolder<Int>::generic<String>
    GenericHolder<Int>::generic<String>(GenericHolder(), "")
    GenericHolder<Int>::property
}

fun callableReferenceWithContextArguments() {
    ::withContext
    ::withContext(LoggerContext(), 1)
    ::withContextProperty
    ::withContextProperty(LoggerContext())
}

// LANGUAGE: +ContextParameters +ExplicitContextArguments
