<!CONFLICTING_OVERLOADS{LT}!><!CONFLICTING_OVERLOADS!>@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
<!HIDDEN!>@kotlin.internal.LowPriorityInOverloadResolution<!>
fun foo(): Int<!> = 1<!>

<!CONFLICTING_OVERLOADS{LT}!><!CONFLICTING_OVERLOADS!>fun foo(): String<!> = ""<!>

fun test() {
    val s = foo()
    s.length
}
