interface A

interface B

fun test1(): B = object : A {
}

fun test2(): B = object {
}