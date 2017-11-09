package dependencies

fun test() {
    val <warning>bar</warning> = A.bar(A.myList) // MISSING_DEPENDENCY_CLASS foo.Foo.Bar
}
