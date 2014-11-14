fun foo(p1: Int?, p2: Int) { }

fun bar(nullable: Int?) {
    foo(null<caret>)
}

// ELEMENT: *
// CHAR: ','
