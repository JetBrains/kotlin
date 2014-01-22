package a

class A() {
}

class B() {
}

fun B.next(): Int = 3

fun B.hasNext(): Boolean = false

<selection>fun A.iterator() = B()

fun f() {
    for (i in A()) {
    }
}</selection>