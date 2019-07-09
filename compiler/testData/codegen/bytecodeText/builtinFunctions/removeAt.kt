// IGNORE_BACKEND: JVM_IR
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
16 INVOKEVIRTUAL A[0-9]+.removeAt \(I\) -> calls in bridges with signature `public final bridge remove\(I\)` + 7 calls from `public synthetic bridge remove\(I\)Ljava/lang/Object;`
9 INVOKEVIRTUAL A[0-9]+\.remove \(I\) -> calls to A1-A9.removeAt
1 INVOKEINTERFACE A9\.remove \(I\) -> call A9.removeAt
1 INVOKEINTERFACE A9\.remove \(Ljava/lang/Object;\) -> call A9.remove
1 INVOKEVIRTUAL A10\.remove \(I\) -> one call in 'box' function
*/

// 16 INVOKEVIRTUAL A[0-9]+.removeAt \(I\)
// 9 INVOKEVIRTUAL A[0-9]+\.remove \(I\)
// 1 INVOKEINTERFACE A9\.remove \(I\)
// 1 INVOKEINTERFACE A9\.remove \(Ljava/lang/Object;\)
// 1 INVOKEVIRTUAL A10\.remove \(I\)
// 2 INVOKEINTERFACE java\/util\/List.remove \(I\)
// 2 INVOKEINTERFACE java\/util\/List.remove \(Ljava/lang/Object;\)

