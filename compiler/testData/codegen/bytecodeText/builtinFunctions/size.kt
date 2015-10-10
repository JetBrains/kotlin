abstract class A1 : Collection<String> {
    override val size: Int get() = 1
}

abstract class A2 : Collection<String> {
    abstract override val size: Int
}

abstract class A3 : java.util.AbstractCollection<String>() {
    override val size: Int get() = 1
}

abstract class A4 : java.util.AbstractCollection<String>() {
    abstract override val size: Int
}

abstract class A5 : java.util.ArrayList<String>() {
    override val size: Int get() = 1
}

abstract class A6 : java.util.ArrayList<String>() {
    abstract override val size: Int
}

abstract class A7 : MutableList<String>
abstract class A8 : java.util.ArrayList<String>()

interface A9 : List<String> {}

fun box(
        a1: A1,
        a2: A2,
        a3: A3,
        a4: A4,
        a5: A5,
        a6: A6,
        a7: A7,
        a8: A8,
        a9: A9,
        c1: Collection<String>,
        c2: MutableCollection<String>
) {
    a1.size
    a2.size
    a3.size
    a4.size
    a5.size
    a6.size
    a7.size
    a8.size
    a9.size
    c1.size
    c2.size
}

/*

*/

// 8 public final bridge size\(\)I
// 8 INVOKEVIRTUAL A[0-9]+\.size \(\)I
// 1 INVOKEINTERFACE A9+\.size \(\)I
// 8 INVOKEVIRTUAL A[0-9]+\.getSize() \(\)I
// 2 INVOKEINTERFACE java\/util\/Collection.size \(\)I
// 4 public abstract getSize\(\)I
