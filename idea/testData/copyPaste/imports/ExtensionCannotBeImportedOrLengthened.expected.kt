// ERROR: Overload resolution ambiguity:  internal fun a.A.ext(): kotlin.Unit defined in a internal fun a.A.ext(): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  internal var a.A.p: kotlin.Int defined in a internal var a.A.p: kotlin.Int defined in to
// ERROR: Overload resolution ambiguity:  internal var a.A.p: kotlin.Int defined in a internal var a.A.p: kotlin.Int defined in to
package to

import a.A
import a.ext
import a.p

fun A.ext() {
}

var A.p: Int
    get() = 2
    set(i: Int) = throw UnsupportedOperationException()

fun A.f() {
    ext()
    p
    p = 3
}