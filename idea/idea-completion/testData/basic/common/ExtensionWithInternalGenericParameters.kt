import java.util.HashSet

open class Base

fun <T: Base> Set<List<T>>.extensionInternal() = 12

fun some() {
    HashSet<List<Base>>().ex<caret>
}

// EXIST: extensionInternal