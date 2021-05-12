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
val test2 = C(1, <!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>C(<!TYPE_MISMATCH, TYPE_MISMATCH!>""<!>, null)<!>)
val test23 = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS!>C2(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>if (true) 1 else null<!>)<!>
val test234 = C2(<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS!>C2(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>if (true) 1 else null<!>)<!>)
