# Interaction of inlining with typeOf function

[typeOf](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/type-of.html) function is a standard
library function, which gets type argument, and returns KType, corresponding with this type argument. 

typeOf function is quite special for inliner, as it has reified type parameter, but is an intrinsic, 
not something, which can be normally inlined  

Basic case of interaction looks like the following:

```kotlin
import kotlin.reflect.*

inline fun <reified T> typeOfValue(x: T) = typeOf<T>()
fun main() {
    println(typeOfValue(1)) // prints int
    println(typeOfValue("a")) // prints java.lang.String
    println(typeOfValue(if (true) listOf(1) else mutableListOf(null))) // prints java.util.List<java.lang.Integer?>
}
```

In interaction with inline functions call-chains it can become trickier

```kotlin
inline fun <K, reified V> typeOfMap() = typeOf<Map<K, V>>()

fun main() {
    println(typeOfMap<Int, Int>()) // prints java.util.Map<K, java.lang.Integer>
}
```

In that case, `K` is neither erased nor substituted. 

This is now handled by doing part of typeOf processing before inlining, as there is no K in callsite context.
On the other side, we can't process `V`, because we don't know it's value yet, so another part must be done after inlining.

For example, for code above, it would be transformed to following intermediate state before inlining

```kotlin
inline fun <K, V> typeOfMap() = KType(classifier = Map::class, typeArguemnts = [KTypeArgument(K), typeOf<V>()])

fun main() {
    println(typeOfMap<Int, Int>()) // prints java.util.Map<K, java.lang.Integer>
}
```

Unfortunately, this doesn't cover all cases correctly. This means, that typeOf handling should be somehow embedded into inlining. 
```kotlin
import kotlin.reflect.*

inline fun <reified T> typeOfValue(x: T) = typeOf<T>()
inline fun <T> typeOfNonReifiedList(x: List<T>) = typeOfValue(x)

fun main() {
    println(typeOfNonReifiedList(listOf(1, 2, 3))) // prints java.util.List<kotlin.Int> on native, but should print java.util.List<T>
}
```
