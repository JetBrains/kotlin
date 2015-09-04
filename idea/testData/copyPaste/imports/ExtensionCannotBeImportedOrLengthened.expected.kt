// ERROR: Overload resolution ambiguity:  public fun a.A.ext(): kotlin.Unit defined in a public fun a.A.ext(): kotlin.Unit defined in to
// ERROR: Overload resolution ambiguity:  public var a.A.p: kotlin.Int defined in a public var a.A.p: kotlin.Int defined in to
// ERROR: Overload resolution ambiguity:  public var a.A.p: kotlin.Int defined in a public var a.A.p: kotlin.Int defined in to
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