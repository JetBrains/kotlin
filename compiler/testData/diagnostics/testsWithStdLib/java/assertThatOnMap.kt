// FIR_IDENTICAL
// SKIP_TXT
// FILE: AbstractMapAssert.java

import java.util.Map;

public abstract class AbstractMapAssert<SELF extends AbstractMapAssert<SELF, ACTUAL, K, V>, ACTUAL extends Map<K, V>, K, V> {
    public SELF isNotNull() {
        return (SELF) this;
    }
}

// FILE: MapAssert.java

import java.util.Map;

public class MapAssert<KEY, VALUE> extends AbstractMapAssert<MapAssert<KEY, VALUE>, Map<KEY, VALUE>, KEY, VALUE> {

    public MapAssert(Map<KEY, VALUE> actual) {}
}

// FILE: Assertions.java

import java.util.Map;

public class Assertions {
    public static <K, V> MapAssert<K, V> assertThat(Map<K, V> actual) {
        return new MapAssert<>(actual);
    }
}

// FILE: test.kt

fun <S : Map<*,*>> S?.must() {
    Assertions.assertThat(this).isNotNull
}
