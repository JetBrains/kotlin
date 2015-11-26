// ERROR: Overload resolution ambiguity:  public fun a.A.ext(): kotlin.Unit defined in a public fun a.A.ext(): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  public operator fun a.A.plus(a: a.A): kotlin.Unit defined in a public operator fun a.A.plus(a: a.A): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  public infix fun a.A.infix(a: a.A): kotlin.Unit defined in a public infix fun a.A.infix(a: a.A): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  public operator fun a.A.unaryMinus(): kotlin.Unit defined in a public operator fun a.A.unaryMinus(): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  public val a.A.p: kotlin.Int defined in a public val a.A.p: kotlin.Int defined in to
package to

import a.A
import a.ext
import a.infix
import a.p
import a.plus
import a.unaryMinus

fun A.ext() {
}

infix fun A.infix(a: A) {
}

operator fun A.plus(a: A) {
}

operator fun A.unaryMinus() {
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