// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// FILE: insideInitBlock.kt
class Outer {
    class Nested {
        class PredicateSource(val condition: Boolean)

        val value: String

        init {
            value = Test.test(PredicateSource::condition, PredicateSource(true))
        }
    }
}

fun box() = Outer.Nested().value

// FILE: Test.java
public class Test {
    public static <T> String test(Predicate<T> predicate, T value) {
        if (predicate.getResult(value) == true)
            return "OK";
        else
            return "Failed";
    }
}

// FILE: Predicate.java
public interface Predicate<T> {
    Boolean getResult(T value);
}
