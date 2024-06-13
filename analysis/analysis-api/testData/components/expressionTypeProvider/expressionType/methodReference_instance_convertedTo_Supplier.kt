// WITH_JDK
import java.util.function.Supplier

class GenericClass<T> { fun foo(): T = TODO() }
val supplier = Supplier<Any>(<expr>GenericClass<String>()::foo</expr>)