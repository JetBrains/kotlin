<caret>tailrec fun foo() {
    foo()
}

//INFO: tailrec marks a function as <a href="https://kotlinlang.org/docs/reference/functions.html#tail-recursive-functions">tail-recursive</a> (allowing the compiler to replace recursion with iteration)