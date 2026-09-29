// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: RawCreate.java
public interface RawCreate {
    Object create(String name, Class type);
}

// FILE: GenericCreate.java
public interface GenericCreate {
    <T> T create(String name, Class<T> type);
}

// FILE: Both.java
public interface Both extends RawCreate, GenericCreate {
    void other();
}

// FILE: Sub.java
public interface Sub extends Both {
    void other();
}

// FILE: box.kt
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals

fun box(): String {
    assertEquals("kotlin.Unit", Sub::class.java.getMethod("other").kotlinFunction!!.returnType.toString())
    return "OK"
}
