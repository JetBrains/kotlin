// "Cast expression 's' to 'String'" "false"
// ACTION: Add 'toString()' call
// ACTION: Add non-null asserted (!!) call
// ACTION: Change parameter 's' type of function 'bar' to 'String?'
// ACTION: Create function 'bar'
// ACTION: Surround with null check
// ACTION: Wrap with '?.let { ... }' call
// ERROR: Type mismatch: inferred type is String? but String was expected

fun foo(s: String?) {
    bar(<caret>s)
}

fun bar(s: String){}