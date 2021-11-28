class A {
    var something: Int
        set(value) {}
        get() = 10
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









