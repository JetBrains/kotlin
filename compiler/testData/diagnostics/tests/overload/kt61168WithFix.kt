// ISSUE: KT-61168

class A<!CONFLICTING_OVERLOADS!>()<!> {
}

<!CONFLICTING_OVERLOADS!>@Deprecated("A", level = DeprecationLevel.HIDDEN)
fun A()<!> = A()
