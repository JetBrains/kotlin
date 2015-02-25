package to

import a.A
import a.B
import a.hasNext
import a.next

fun A.iterator() = B()

fun f() {
    for (i in A()) {
    }
}