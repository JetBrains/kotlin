// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: Test.java

class Test {
    interface A {
        boolean add(String s);
    }

    static class D extends C {}

    void test() {
        A a = new D();
        a.add("lol");
    }
}

// FILE: test.kt

abstract class C : Test.A, List<String> {
    override val size: Int get() = null!!
    override fun isEmpty(): Boolean = null!!
    override fun contains(o: String): Boolean = null!!
    override fun iterator(): Iterator<String> = null!!
    override fun containsAll(c: Collection<String>): Boolean = null!!
    override fun get(index: Int): String = null!!
    override fun indexOf(o: String): Int = null!!
    override fun lastIndexOf(o: String): Int = null!!
    override fun listIterator(): ListIterator<String> = null!!
    override fun listIterator(index: Int): ListIterator<String> = null!!
    override fun subList(fromIndex: Int, toIndex: Int): List<String> = null!!
}

fun box(): String {
    try {
        Test().test()
        return "Fail"
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
}
