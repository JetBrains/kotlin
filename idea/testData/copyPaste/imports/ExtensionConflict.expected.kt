// ERROR: Overload resolution ambiguity:  public fun a.A.ext(): kotlin.Unit defined in a public fun a.A.ext(): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  public fun a.A.plus(a: a.A): kotlin.Unit defined in a public fun a.A.plus(a: a.A): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  public fun a.A.infix(a: a.A): kotlin.Unit defined in a public fun a.A.infix(a: a.A): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  public fun a.A.minus(): kotlin.Unit defined in a public fun a.A.minus(): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  public val a.A.p: kotlin.Int defined in a public val a.A.p: kotlin.Int defined in to
package to

import a.A
import a.ext
import a.infix
import a.minus
import a.p
import a.plus

fun A.ext() {
}

fun A.infix(a: A) {
}

fun A.plus(a: A) {
}

fun A.minus() {
}

val A.p: Int
    get() = 2

fun f() {
    A().ext()
    A() + A()
    A() infix A()
    -A()
    A().p
}