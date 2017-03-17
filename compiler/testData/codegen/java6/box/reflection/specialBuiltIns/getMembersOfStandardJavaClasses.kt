// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT
// FULL_JDK
// See KT-11258 Incorrect resolution sequence for Java field

//SHOULD BE deleted after KT-16616 fix
import java.util.*

fun box(): String {
    listOf(
            ArrayList::class,
            LinkedList::class,
            AbstractList::class,
            HashSet::class,
            TreeSet::class,
            HashMap::class,
            TreeMap::class,
            AbstractMap::class,
            AbstractMap.SimpleEntry::class
    ).map {
        it.members.map(Any::toString)
    }
    return "OK"
}
