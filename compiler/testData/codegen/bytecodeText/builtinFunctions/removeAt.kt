// JVM_ABI_K1_K2_DIFF: KT-63857

abstract class A1<T> : MutableList<T> {
    override fun remove(x: T): Boolean = true
    override fun removeAt(index: Int): T = null!!
}

abstract class A2 : MutableList<String> {
    override fun remove(x: String): Boolean = true
    override fun removeAt(index: Int): String = null!!
}

abstract class A3 : java.util.AbstractList<String>() {
    override fun remove(x: String): Boolean = true
    override fun removeAt(index: Int): String = null!!
}

abstract class A4 : java.util.AbstractList<String>() {
    override abstract fun remove(x: String): Boolean
    override abstract fun removeAt(index: Int): String
}

abstract class A5 : java.util.ArrayList<String>() {
    override fun remove(x: String): Boolean = true
    override fun removeAt(index: Int): String = null!!
}

abstract class A6 : java.util.ArrayList<String>() {
    override abstract fun remove(x: String): Boolean
    override abstract fun removeAt(index: Int): String
}

abstract class A7 : MutableList<String>
abstract class A8 : java.util.ArrayList<String>()

interface A9 : MutableList<String> {}

abstract class A10 : MutableList<Int> {
    override fun remove(x: Int): Boolean = true
    override fun removeAt(index: Int): Int = 1
}

fun box(
        a1: A1<String>,
        a2: A2,
        a3: A3,
        a4: A4,
        a5: A5,
        a6: A6,
        a7: A7,
        a8: A8,
        a9: A9,
        a10: A10,
        c1: MutableList<String>,
        c2: MutableList<Int>
) {
    a1.removeAt(1)
    a1.remove("")

    a2.removeAt(1)
    a2.remove("")

    a3.removeAt(1)
    a3.remove("")

    a4.removeAt(1)
    a4.remove("")

    a5.removeAt(1)
    a5.remove("")

    a6.removeAt(1)
    a6.remove("")

    a7.removeAt(1)
    a7.remove("")

    a8.removeAt(1)
    a8.remove("")

    a9.removeAt(1)
    a9.remove("")

    a10.removeAt(1)
    a10.remove(2)

    c1.removeAt(1)
    c1.remove("")

    c2.removeAt(1)
    c2.remove(2)
}

/*
1 INVOKEINTERFACE A9\.remove \(I\) -> call A9.removeAt
1 INVOKEINTERFACE A9\.remove \(Ljava/lang/Object;\) -> call A9.remove

On the JVM backend we have:
16 INVOKEVIRTUAL A[0-9]+.removeAt \(I\) -> calls in bridges with signature `public final bridge remove\(I\)` + 7 calls from `public synthetic bridge remove\(I\)Ljava/lang/Object;`
9 INVOKEVIRTUAL A[0-9]+\.remove \(I\) -> calls to A1-A9.removeAt
1 INVOKEVIRTUAL A10\.remove \(I\) -> one call in 'box' function

On the JVM IR backend we have:
10 INVOKEVIRTUAL A[0-9]+.removeAt \(I\) -> calls in bridges with signature `public final bridge remove(I)`
15 INVOKEVIRTUAL A[0-9]+\.remove \(I\) -> calls to A1-A9.removeAt + calls in synthetic bridges with signature `public synthetic bridge remove(I)Ljava/lang/Object;`
2 INVOKEVIRTUAL A10\.remove \(I\) -> one call in 'box' function + call from synthetic `remove(I)` bridge

This currently differs because of KT-40277, and the test expectations should be revised once KT-40277 is resolved.
*/

// 1 INVOKEINTERFACE A9\.remove \(I\)
// 1 INVOKEINTERFACE A9\.remove \(Ljava/lang/Object;\)
// 2 INVOKEINTERFACE java\/util\/List.remove \(I\)
// 2 INVOKEINTERFACE java\/util\/List.remove \(Ljava/lang/Object;\)

// JVM_TEMPLATES:
// 16 INVOKEVIRTUAL A[0-9]+.removeAt \(I\)
// 9 INVOKEVIRTUAL A[0-9]+\.remove \(I\)
// 1 INVOKEVIRTUAL A10\.remove \(I\)

// JVM_IR_TEMPLATES:
// 1 INVOKEVIRTUAL A1\.removeAt \(I\)
// 1 INVOKEVIRTUAL A2\.removeAt \(I\)
// 1 INVOKEVIRTUAL A3\.removeAt \(I\)
// 1 INVOKEVIRTUAL A4\.removeAt \(I\)
// 1 INVOKEVIRTUAL A5\.removeAt \(I\)
// 1 INVOKEVIRTUAL A6\.removeAt \(I\)
// 1 INVOKEVIRTUAL A8\.removeAt \(I\)
// 1 INVOKEVIRTUAL A10\.removeAt \(I\)

// 2 INVOKEVIRTUAL A7\.removeAt \(I\)
//  ^ in:
//      public final bridge remove(I)Ljava/lang/String;
//      public synthetic bridge remove(I)Ljava/lang/Object;

// 10 INVOKEVIRTUAL A[0-9]+\.removeAt \(I\)

// 15 INVOKEVIRTUAL A[0-9]+\.remove \(I\)
// 2 INVOKEVIRTUAL A10\.remove \(I\)
