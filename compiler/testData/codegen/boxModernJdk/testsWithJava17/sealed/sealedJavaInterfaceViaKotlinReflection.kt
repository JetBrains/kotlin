// WITH_REFLECT
// FILE: Base.java
public sealed interface Base permits O, K {}

// FILE: O.java
public non-sealed interface O extends Base {}

// FILE: K.java
public non-sealed interface K extends Base {}

// FILE: main.kt

fun box(): String {
    val klass = Base::class
    if (!klass.isSealed) return "Error: Base is not sealed"
    if (klass.isAbstract) return "Error: Base is abstract"
    return klass.sealedSubclasses.asReversed()
        .joinToString("") { it.simpleName ?: "_No name provided_" }
        .takeIf { it.isNotBlank() }
        ?: "_No sealed subclasses found_"
}
