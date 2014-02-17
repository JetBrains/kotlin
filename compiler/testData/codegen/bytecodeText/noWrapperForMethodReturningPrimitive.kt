class A {
    fun foo(): Int = 42
}

fun bar() = A().foo()

// Test that for a simple final method in a final class we don't generate a bridge returning wrapper type
// (we only generate the method returning a primitive int)

// 0 foo\(\)Ljava/lang/Integer;
