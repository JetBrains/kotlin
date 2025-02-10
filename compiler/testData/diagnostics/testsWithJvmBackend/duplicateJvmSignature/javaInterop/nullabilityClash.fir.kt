// JVM_DEFAULT_MODE: no-compatibility
// JVM_TARGET: 1.8
// ISSUE: KT-74209

// FILE: I1.java
import org.jetbrains.annotations.NotNull;

public interface I1<T1> extends I0<T1> {
    @Override
    default Object func(@NotNull T1 t1) {
        return t1;
    }
}

// FILE: main.kt
interface I0<T0> {
    fun func(t0: T0): Any?
}

interface I2<T2>: I0<T2>, I1<T2> {
    override fun func(t0: T2): Any? {
        return Any()
    }
}
