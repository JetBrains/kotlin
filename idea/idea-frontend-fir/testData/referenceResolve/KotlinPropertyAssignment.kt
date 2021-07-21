class A {
    var something: Int = 10
}

fun A.foo(a: A) {
    print(a.<caret>something)
    a.<caret>something = 1
    a.<caret>something += 1
    a.<caret>something++
    --a.<caret>something

    <caret>something++
    (<caret>something)++
    (<caret>something) = 1
    (a.<caret>something) = 1
}

// MULTIRESOLVE
// REF1: (in A).something
// REF2: (in A).something
// REF3: (in A).something
// REF4: (in A).something
// REF5: (in A).something
// REF6: (in A).something
// REF7: (in A).something
// REF8: (in A).something
// REF9: (in A).something
