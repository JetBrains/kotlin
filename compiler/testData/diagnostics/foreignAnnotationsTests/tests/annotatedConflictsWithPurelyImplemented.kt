// FIR_IDENTICAL
// WITH_STDLIB
// FULL_JDK
// FILE: J1.java
import androidx.annotation.*;
import java.util.Map;

public abstract class J1<K, V> implements Map<K, V> {
    @Override
    public V remove(@RecentlyNullable Object x) {
        return null;
    }
}

// FILE: J2.java
import androidx.annotation.*;
import java.util.Map;

@kotlin.jvm.PurelyImplements("kotlin.collections.MutableMap")
public abstract class J2<K, V> extends J1<K, V> implements Map<K, V> {
    @Override
    public V remove(@RecentlyNullable Object x) {
        return null;
    }
}

// FILE: test.kt
abstract class K : J2<String, String>() {
    override fun remove(key: String): String? {
        return null
    }
}