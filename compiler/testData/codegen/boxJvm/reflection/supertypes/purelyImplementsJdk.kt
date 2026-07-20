// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// FULL_JDK

import java.util.*
import java.util.concurrent.*
import java.util.function.*
import kotlin.reflect.KClass

fun checkSupertype(klass: KClass<*>, expectedSupertype: String) {
    val supertypes = klass.supertypes.toString()
    if (expectedSupertype !in supertypes) {
        throw AssertionError("Purely implemented supertype '$expectedSupertype' not found: $supertypes")
    }

    val classifierFqName = expectedSupertype.substringBefore('<')
    if (supertypes.split(classifierFqName).size != 2) {
        throw AssertionError("Superclass $classifierFqName appears more than once in supertypes: $supertypes")
    }
}

fun box(): String {
    checkSupertype(ArrayList::class, "kotlin.collections.MutableList<E>")
    checkSupertype(LinkedList::class, "kotlin.collections.MutableList<E>")

    checkSupertype(HashSet::class, "kotlin.collections.MutableSet<E>")
    checkSupertype(TreeSet::class, "kotlin.collections.MutableSet<E>")
    checkSupertype(LinkedHashSet::class, "kotlin.collections.MutableSet<E>")

    checkSupertype(HashMap::class, "kotlin.collections.MutableMap<K, V>")
    checkSupertype(TreeMap::class, "kotlin.collections.MutableMap<K, V>")
    checkSupertype(LinkedHashMap::class, "kotlin.collections.MutableMap<K, V>")
    checkSupertype(ConcurrentHashMap::class, "kotlin.collections.MutableMap<K, V>")
    checkSupertype(ConcurrentSkipListMap::class, "kotlin.collections.MutableMap<K, V>")

    checkSupertype(UnaryOperator::class, "java.util.function.Function<T, T>")
    checkSupertype(BinaryOperator::class, "java.util.function.BiFunction<T, T, T>")

    return "OK"
}
