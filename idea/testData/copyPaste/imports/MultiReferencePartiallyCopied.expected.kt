package to

import a.next
import a.hasNext
import a.A
import a.B

fun A.iterator() = B()

fun f() {
    for (i in A()) {
    }
}