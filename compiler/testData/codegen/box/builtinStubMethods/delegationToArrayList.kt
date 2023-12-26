// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-62550, KT-63828

import java.util.ArrayList

class A<E> : List<E> by ArrayList<E>()

class B : List<String> by A<String>()

fun expectUoe(block: () -> Any) {
    try {
        block()
        throw AssertionError()
    } catch (e: UnsupportedOperationException) {
    }
}

fun box(): String {
    val a = A<String>() as java.util.List<String>
    expectUoe { a.add("") }
    expectUoe { a.remove("") }
    expectUoe { a.addAll(a) }
    expectUoe { a.addAll(0, a) }
    expectUoe { a.removeAll(a) }
    expectUoe { a.retainAll(a) }
    expectUoe { a.clear() }
    expectUoe { a.add(0, "") }
    expectUoe { a.set(0, "") }
    expectUoe { a.remove(0) }
    a.listIterator()
    a.listIterator(0)
    a.subList(0, 0)

    val b = B() as java.util.List<String>
    expectUoe { b.add("") }
    expectUoe { b.remove("") }
    expectUoe { b.addAll(b) }
    expectUoe { b.addAll(0, b) }
    expectUoe { b.removeAll(b) }
    expectUoe { b.retainAll(b) }
    expectUoe { b.clear() }
    expectUoe { b.add(0, "") }
    expectUoe { b.set(0, "") }
    expectUoe { b.remove(0) }
    b.listIterator()
    b.listIterator(0)
    b.subList(0, 0)

    return "OK"
}
