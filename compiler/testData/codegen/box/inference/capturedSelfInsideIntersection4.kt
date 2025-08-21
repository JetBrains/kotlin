// TARGET_BACKEND: JVM_IR
// ISSUE: KT-74999
// WITH_STDLIB

// FILE: GraphQlTester.java
public interface GraphQlTester {
    public static interface Traversable {}
    public static interface Entity<D, S extends Entity<D, S>> extends Traversable {
        <T extends S> T isEqualTo(Object expected);
    }
    public static interface Path extends Traversable {
        Path valueIsNull();
        <E> GraphQlTester.Entity<E, ?> entity(Class<E> entityType);
    }
}

// FILE: test.kt

inline fun <reified U> GraphQlTester.Path.isEqualTo(expected: U?) {
    if (null == expected) valueIsNull()
    // Type parameter T of isEqualTo is inferred to Nothing
    else entity(U::class.java).isEqualTo(expected)
}

open class EntityImpl<D> : GraphQlTester.Entity<D, EntityImpl<D>> {
    override fun <T : EntityImpl<D>> isEqualTo(expected: Any?): T = this as T
}

class PathImpl : GraphQlTester.Path {
    override fun valueIsNull() = this

    override fun <E> entity(entityType: Class<E>): GraphQlTester.Entity<E, *> =
        EntityImpl<E>()
}

fun box(): String {
    PathImpl().isEqualTo(42)
    return "OK"
}
