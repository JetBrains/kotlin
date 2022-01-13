// SCOPE_DUMP: C:getName;setName;name, D:getName;setName;name
// FILE: A.java
public interface A {
    String getName();
}

// FILE: B.java
public interface B<T> extends A {
    T setName(String newName);
}

// FILE: C.kt
open class C(private var name: String) : B<Any?> {
    override fun getName(): String = name
    override fun setName(newName: String): Any? = null
}

// FILE: D.java
public class D extends C {}

// FILE: main.kt
fun test(d: D) {
    val name = d.name
}

