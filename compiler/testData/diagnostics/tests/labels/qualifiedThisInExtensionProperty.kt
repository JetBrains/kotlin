// WITH_STDLIB
// ISSUE: KT-20617

fun Int.function() = run {
    this@function + 1
}

val Int.property1: Int
    get() {
        return run { this@property1 + 1 }
    }

val Int.property2 get() = run {
    this<!UNRESOLVED_REFERENCE!>@property2<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 1
}
