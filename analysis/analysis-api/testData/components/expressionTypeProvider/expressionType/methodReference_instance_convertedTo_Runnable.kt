// WITH_JDK
import java.lang.Runnable

class GenericClass<T> { fun foo(): T = TODO() }
val runnable = Runnable(<expr>GenericClass<String>()::foo</expr>)