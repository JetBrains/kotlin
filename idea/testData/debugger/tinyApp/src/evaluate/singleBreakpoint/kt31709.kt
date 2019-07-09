package kt31709

data class A(val a: Int)

fun main() {
    with(A(2)) {
        //Breakpoint!
        listOf(1, 2, 3, 4, 5).filter { it > a }
    }
}

// EXPRESSION: listOf(1, 2, 3, 4, 5).filter { it > a }
// RESULT: instance of java.util.ArrayList(id=ID): Ljava/util/ArrayList;