// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: SingletonCollection.kt
package test

open class SingletonCollection<T>(val value: T) : AbstractCollection<T>() {
    override val size = 1
    override fun iterator(): Iterator<T> = listOf(value).iterator()

    protected override fun toArray(): Array<Any?> =
            arrayOf<Any?>(value)

    protected override fun <E> toArray(a: Array<E>): Array<E> {
        a[0] = value as E
        return a
    }
}

// FILE: JavaSingletonCollection.java
import test.*;

public class JavaSingletonCollection<T> extends SingletonCollection<T> {
    public JavaSingletonCollection(T value) {
        super(value);
    }
}

// FILE: JavaSingletonCollection2.java
import test.*;

public class JavaSingletonCollection2<T> extends SingletonCollection<T> {
    public JavaSingletonCollection2(T value) {
        super(value);
    }

    public Object[] toArray() {
        return super.toArray();
    }

    public <E> E[] toArray(E[] arr) {
        return super.toArray(arr);
    }
}


// FILE: box.kt
import test.*

fun box(): String {
    val jsc = JavaSingletonCollection(42) as java.util.Collection<Int>
    val test3 = jsc.toArray()
    if (test3[0] != 42) return "Failed #3"

    val test4 = arrayOf<Any?>(0)
    jsc.toArray(test4)
    if (test4[0] != 42) return "Failed #4"

    val jsc2 = JavaSingletonCollection2(42) as java.util.Collection<Int>
    val test5 = jsc2.toArray()
    if (test5[0] != 42) return "Failed #5"

    val test6 = arrayOf<Any?>(0)
    jsc2.toArray(test6)
    if (test6[0] != 42) return "Failed #6"

    return "OK"
}
