import java.util.ArrayList

public class A(val a: Int, var b: Int = 3, val arr: Array<Int>, val lst: ArrayList<String> = arrayListOf("one", "two", "3")) {
    val c: Int
    var arr2: IntArray
    init {
        c = a + b
        arr2 = IntArray(c)
    }

    public constructor(a: Int = 3): this(a, arr = Array(a, { it }), lst = arrayListOf("a")) {
        println(lst[a])
    }

    companion object {
        public val ARR: IntArray = IntArray(10)
    }

    public fun withLocalArray(): Int {
        val i = 3
        val arr3 = arrayOf(1, 2, 3)
        val d = <!OUT_OF_BOUND_ACCESS!>arr3[i]<!> + arr3[0] + arr3[a] + arr3[b] + arr3[c]
        b = 0
        val f = arr3[b]
        b = 150
        val g = arr3[b]
        return d + f + g
    }

    // below class member arrays are used, we have no info about them, so no errors reported
    public fun with_arr(): Int {
        val i = 3
        val d = arr[i] + arr[0] + arr[a] + arr[b] + arr[c]
        b = 0
        val f = arr[b]
        b = 150
        val g = arr[b]
        return d + f + g
    }

    public fun with_arr2(): Int {
        val i = 3
        val d = arr2[i] + arr2[0] + arr2[a] + arr2[b] + arr2[c]
        b = 0
        val f = arr2[b]
        b = 150
        val g = arr2[b]
        return d + f + g
    }

    public fun with_lst(): String {
        val i = 3
        val d = lst[i] + lst[0] + lst[a] + lst[b] + lst[c]
        b = 0
        val f = lst[b]
        b = 150
        val g = lst[b]
        return d + f + g
    }

    public fun with_ARR(): Int {
        val i = 3
        val d = A.ARR[i] + A.ARR[0] + A.ARR[a] + A.ARR[b] + A.ARR[c]
        b = 0
        val f = A.ARR[b]
        b = 150
        val g = A.ARR[b]
        return d + f + g
    }
}

fun withLocalClass(): Int {
    // wunctions with local classes are not processed
    class A () {
        fun foo(): Int {
            val arr = arrayListOf(3)
            return arr[3]
        }
    }
    class B (val arr: IntArray)

    val a = A()
    val b = B(IntArray(4))
    return a.foo() + b.arr[5]
}

public data class WithVarC(var c: Int)
public data class WithVarCAndArray(var c: Int, var array: IntArray)

fun classMutableField(): Int {
    val a = WithVarC(3)
    val b = WithVarCAndArray(3, IntArray(3))
    var c = 3
    val r1 = b.array[a.c] + b.array[b.c] + b.array[c]
    a.c = 3
    b.c = 4
    b.array = IntArray(3)
    return r1 + b.array[a.c] + b.array[b.c]
}

fun closureInLocalClass() {
    // wunctions with local classes are not processed
    val arr = arrayOf(1)
    var c = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>0<!>
    class B () {
        init {
            c = 100
        }
        public constructor(<!UNUSED_PARAMETER!>arg<!>: Int) : this() {
            c = 0
        }
        var a: Int
            get() {
                c = 0
                return 1
            }
            set(value) { a = value }

        fun boo() {
            c = 0
        }
    }
    arr[c]

    c = 100
    val b = B(3)
    arr[c]

    b.a
    arr[c]

    b.a = 3
    arr[c]

    b.boo()
    arr[c]

    c = 100
    val obj = object : Comparable<Int> {
        override fun compareTo(other: Int): Int {
            c = 0
            return 0
        }
    }
    obj.compareTo(3)
    arr[c]
}