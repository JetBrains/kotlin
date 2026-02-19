// TARGET_BACKEND: JVM_IR

// FILE: Base.java
public class Base<T> {
    protected T s = (T) "OK";
}

// FILE: Derived.kt
open class Derived<K> : Base<K>()

// FILE: Impl.java
public class Impl extends Derived<String> {}

// FILE: app.kt
class RealImpl : Impl() {
    fun foo(): String = s
}

fun box(): String {
    return RealImpl().foo()
}
