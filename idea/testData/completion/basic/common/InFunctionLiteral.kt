fun foo(p: Iterable<D>) {
    p.filter { it.<caret> }
}

trait D {
    fun bar()
}

// EXIST: bar
