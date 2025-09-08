// TARGET_BACKEND: JVM_IR
// FULL_JDK
// ISSUE: KT-79816

// FILE: JavaSupplier.java
public interface JavaSupplier<V> {
    public V get();
}

// FILE: main.kt
import java.util.function.Supplier

fun interface KotlinSupplier<V> {
    fun get(): V
}

class KotlinSupplierProxy<V>(supplier: KotlinSupplier<V>) : KotlinSupplier<V> by supplier
class JavaManualSupplierProxy<V>(supplier: JavaSupplier<V>) : JavaSupplier<V> by supplier
class JavaStdlibSupplierProxy<V>(supplier: Supplier<V>) : Supplier<V> by supplier

fun requireNull(x: Any?) {
    if (x != null) {
        throw IllegalStateException("Value is not null: $x")
    }
}

fun box(): String {
    val nullKotlinSupplier = KotlinSupplier<Boolean?> { null }
    val kotlinProxy = KotlinSupplierProxy(nullKotlinSupplier)
    requireNull(kotlinProxy.get())

    val nullManualJavaSupplier = JavaSupplier<Boolean?> { null }
    val javaManualProxy = JavaManualSupplierProxy(nullManualJavaSupplier)
    requireNull(javaManualProxy.get())

    val nullJavaStdlibSupplier = Supplier<Boolean?> { null }
    val javaStdlibProxy = JavaStdlibSupplierProxy(nullJavaStdlibSupplier)
    requireNull(javaStdlibProxy.get())

    return "OK"
}
