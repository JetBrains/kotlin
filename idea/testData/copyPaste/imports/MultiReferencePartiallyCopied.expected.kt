package to

import a.A
import a.B
import a.next
import a.hasNext

fun A.iterator() = B()

fun f() {
    for (i in A()) {
    }
}