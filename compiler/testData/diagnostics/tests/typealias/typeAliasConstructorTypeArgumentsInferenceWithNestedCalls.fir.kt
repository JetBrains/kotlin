// !LANGUAGE: -TypeEnhancementImprovementsInStrictMode
// FULL_JDK

// FILE: MapLike.java
import java.util.Map;

public class MapLike<@org.jetbrains.annotations.NotNull K> {
    MapLike(K x) {  }
}

// FILE: main.kt
class Cons<T : Number>(val head: T, val tail: Cons<T>?)
typealias C<T> = Cons<T>
typealias C2<T> = MapLike<T>

val test1 = C(1, C(2, null))
val test2 = C(1, C("", null))
val test23 = C2(if (true) 1 else null)
val test234 = C2(C2(if (true) 1 else null))
