// SKIP_JDK6
// IGNORE_BACKEND: JVM_IR
// FILE: Custom.java

class Custom<K, V> {
    private K k;

    private V v;

    public Custom(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public interface MBiConsumer<T, U> {
        void accept(T t, U u);
    }

    public void forEach(MBiConsumer<? super K, ? super V> action) {
        action.accept(k, v);
    }
}

// FILE: 1.kt

import java.util.Arrays

fun box(): String {
    val instance = Custom<String, String>("O", "K")
    var result = "fail"
    instance.forEach { a, b ->
        result = a + b
    }

    val superInterfaces = Arrays.toString((Class.forName("_1Kt\$box$1")).genericInterfaces)
    if (superInterfaces != "[Custom\$MBiConsumer<java.lang.String, java.lang.String>]") {
        return "fail: $superInterfaces"
    }

    return result
}
