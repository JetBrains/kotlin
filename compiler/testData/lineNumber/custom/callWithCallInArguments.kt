class A {
    fun foo(a: A) = a
}

fun bar(a: A) = A()

fun foo() {
    val a = A()
    bar(
            bar(
                    bar(a)
            )
    )

}

// 2 1 5 8 9 +10 +11 10 9 15