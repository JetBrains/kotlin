// WITH_REFLECT
// FILE: Base.java
public sealed class Base permits O, K {}

// FILE: O.java
public final class O extends Base {}

// FILE: K.java
public non-sealed class K extends Base {}

// FILE: main.kt

fun box(): String {
    val clazz = Base::class.java
    if (!clazz.isSealed) return "Error: Base is not sealed"
    return clazz.permittedSubclasses.joinToString("") { it.simpleName ?: "_No name provided_" }
}
