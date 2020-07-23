@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package test

import kotlin.internal.RequireKotlin

<!HIDDEN!>@RequireKotlin("")<!>
fun f01() {}

<!HIDDEN!>@RequireKotlin("x")<!>
fun f02() {}

<!HIDDEN!>@RequireKotlin("1")<!>
fun f03() {}

<!HIDDEN!>@RequireKotlin("1.0-beta")<!>
fun f04() {}

<!HIDDEN!>@RequireKotlin("1.1.0-dev-1111")<!>
fun f05() {}

<!HIDDEN!>@RequireKotlin("1.5.3.7")<!>
fun f06() {}

<!HIDDEN!>@RequireKotlin("1..0")<!>
fun f07() {}

<!HIDDEN!>@RequireKotlin(" 1.0")<!>
fun f08() {}


<!HIDDEN!>@RequireKotlin("1.1")<!>
fun ok1() {}

<!HIDDEN!>@RequireKotlin("1.1.0")<!>
fun ok2() {}

<!HIDDEN!>@RequireKotlin("0.0.0")<!>
fun ok3() {}
