trait A

trait B

fun test1(): B = <!TYPE_MISMATCH!>object : A<!> {
}

fun test2(): B = <!TYPE_MISMATCH!>object<!> {
}