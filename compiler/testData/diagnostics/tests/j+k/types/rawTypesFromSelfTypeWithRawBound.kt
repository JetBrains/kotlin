// ISSUE: KT-55953

// FILE: Invariant.java
public class Invariant<T> {}

// FILE: Generic.java
import java.util.ArrayList;
import java.util.List;

public class Generic<T> {
    public Generic raw = new Generic();
    public static Generic staticRaw = new Generic();
    public Invariant<String> getStringInvariant() { return new Invariant<>(); }
    public List<String> getListOfStrings() { return new ArrayList<>(); }
}

// FILE: GenericBox.java
public class GenericBox<T extends Generic> {
    public T raw = (T) new Generic();
}

// FILE: main.kt
fun `acquire raw type by static field`() {
    val raw = Generic.staticRaw
    val nullableAnyInvariant: Invariant<Any?> = raw.getStringInvariant()
    val anyInvariant: Invariant<Any> = raw.getStringInvariant()
    val listOfNullableAny: List<Any?> = raw.getListOfStrings()
    val listOfAny: List<Any> = <!TYPE_MISMATCH!>raw.getListOfStrings()<!> // K1 & K2: inferred type is (Mutable)List<(raw) Any?>!
}

fun `acquire raw type by instance field`(instance: Generic<*>) {
    val raw = instance.raw
    val nullableAnyInvariant: Invariant<Any?> = raw.getStringInvariant()
    val anyInvariant: Invariant<Any> = raw.getStringInvariant()
    val listOfNullableAny: List<Any?> = raw.getListOfStrings()
    val listOfAny: List<Any> = <!TYPE_MISMATCH!>raw.getListOfStrings()<!> // K1 & K2: inferred type is (Mutable)List<(raw) Any?>!
}

fun `acquire raw type via type parameter's upper bound of another class`(instance: GenericBox<*>) {
    val raw = instance.raw
    val nullableAnyInvariant: Invariant<Any?> = <!TYPE_MISMATCH!>raw.getStringInvariant()<!> // K1 & K2: error
    val anyInvariant: Invariant<Any> = <!TYPE_MISMATCH!>raw.getStringInvariant()<!> // K1 & K2: error
    val listOfNullableAny: List<Any?> = raw.getListOfStrings()
    val listOfAny: List<Any> = raw.getListOfStrings() // K1 & K2: ok
}
