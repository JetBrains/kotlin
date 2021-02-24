// KOTLIN_CONFIGURATION_FLAGS: SAM_CONVERSIONS=CLASS
// WITH_SIGNATURES
// FILE: t.kt
fun main(x: DataStream<Int>) {
    x.keyBy({ it.toLong() })
    x.keyBy(KeySelector<Int, Long>{ it.toLong() })
}

// FILE: KeySelector.java
public interface KeySelector<IN, KEY> {
    KEY getKey(IN value);
}

// FILE: DataStream.java
public class DataStream<T> {
    public <K> void keyBy(KeySelector<T,K> key) {}
}
