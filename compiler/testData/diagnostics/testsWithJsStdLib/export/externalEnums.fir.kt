external enum class A {
    B, compareTo, toString, equals, hashCode
}

fun foo() {
    A.B
    A.compareTo
    A.toString
    A.equals
    A.hashCode

    A.values()
    A.valueOf("B")

    A.B.name
    A.B.ordinal
    A.B.compareTo(A.B)
    A.B.hashCode()

    println(A.B == A.compareTo)

    with(A.B) {
        println(name)
        println(ordinal)
        println(compareTo(A.B))
        println(hashCode())

        println(::name)
        println(::ordinal)
        println(::compareTo)
        println(::hashCode)
    }
}
