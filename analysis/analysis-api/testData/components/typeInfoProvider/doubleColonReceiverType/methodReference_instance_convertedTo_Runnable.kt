// WITH_JDK
import java.lang.Runnable

class GenericClass<T> { fun foo(): T = TODO() }
val runnable = Runnable(GenericClass<String>():<caret>:foo)