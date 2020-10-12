// WITH_SIGNATURES
// FILE: implementsJavaMap.kt
import java.util.*

abstract class JMapImpl<A, B> : JMap<A, B> {
    override fun containsKey(key: A): Boolean = false
}

abstract class JMapNImpl<A : Number, B> : JMapN<A, B> {
    override fun containsKey(key: A): Boolean = false
}

// FILE: JMap.java
import java.util.*;

public interface JMap<K, V> extends Map<K, V> {}

// FILE: JMapN.java
import java.util.*;

public interface JMapN<K extends Number, V> extends Map<K, V> {}