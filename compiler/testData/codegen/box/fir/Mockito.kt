// ISSUE: KT-58893
// TARGET_BACKEND: JVM
// WITH_STDLIB
// IGNORE_INLINER: IR
// JVM_ABI_K1_K2_DIFF: KT-63855

// FILE: InOrder.java

public class InOrder {
    <T> T verify(T mock) {
        String s = mock.getClass().toString();
        if (!s.equals(TestKt.expected)) throw new IllegalStateException(s);
        return mock;
    }
}

// FILE: test.kt

inline fun <reified T : Any> mock(s: String = ""): T = MOCK as T

val MOCK = {}

inline fun inOrder(
    vararg mocks: Any,
    evaluation: InOrder.() -> Unit
) {
    inOrder.evaluation()
    InOrder().evaluation()
}

val inOrder = object : InOrder() {
    override fun <T : Any?> verify(mock: T): T {
        val s = mock!!.javaClass.toString()
        if (s != expected) throw IllegalStateException(s)
        return mock
    }
}

lateinit var expected: String

fun box(): String {
    val mock = mock<() -> Unit>()
    expected = mock.javaClass.toString()
    inOrder(mock) {
        verify(mock)()
    }
    return "OK"
}
