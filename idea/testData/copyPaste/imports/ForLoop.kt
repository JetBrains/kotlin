package a

class A() {
}

class B() {
}

fun B.next(): Int = 3

fun B.hasNext(): Boolean = false

fun A.iterator() = B()

<selection>fun f() {
    for (i in A()) {
    }
}</selection>