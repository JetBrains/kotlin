// WITH_MESSAGE: "Removed 1 import"
import pack1.a
import pack1.a as a1
import kotlin.run as run1
import kotlin.run

fun foo() {
    run1 {}
    a()
    42.a1()
}