class A {
    var something: Int
        set(value) {}
        get() = 10
}

fun A.foo(a: A) {
    print(a.<caret_1>something)
    a.<caret_2>something = 1
    a.<caret_3>something += 1
    a.<caret_4>something++
    --a.<caret_5>something

    <caret_6>something++

    a?.<caret_10>something += 1
}

operator fun Int?.plus(i: Int): Int = 0
