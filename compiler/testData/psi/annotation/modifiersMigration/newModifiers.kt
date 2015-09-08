data annotation tailrec external noinline fun bar(data x: Int) {
    data inline noinline class A

    inline fun foo() {}

    noinline val x1 = 1

    data();

    val x2 = 2

    data;

    val x3 = 3

    inline


    private
    val x4 = 4

    abstract

    data

    class Q
}


fun foo1() {
    data()

    inline data annotation // infix call
}

fun foo2() {
    data {

    }

    inline(data) {

    }
}


public data inline class A {
    val x: Int
    inline data set
    noinline get

    val y: String
    inline get() = 1
    data set(q: Int) = 2

    val z: Double inline get noinline set

    val z0: Double = 3.0
    inline get noinline set
}
