// NAME_COUNT_TO_USE_STAR_IMPORT: 2
package bug.b

import bug.a.*
import bug.a.invoke

fun A.foo() = println("extension function")

fun main(args: Array<String>) {
    val a = A(MyFunction())

    a.foo()
}