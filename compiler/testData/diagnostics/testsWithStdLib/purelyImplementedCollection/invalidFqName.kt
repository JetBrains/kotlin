// !DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: A.java
import java.util.*

@kotlin.jvm.PurelyImplements("")
public class A<T> extends AbstractList<T> {
    @Override
    public T get(int index) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }
}

// FILE: B.java
import java.util.*

@kotlin.jvm.PurelyImplements("[INVALID]")
public class B<T> extends AbstractList<T> {}

// FILE: main.kt
val x = A<String>()
val y = B<String>()