external enum class A {
    B, compareTo, toString, equals, hashCode
}

fun foo() {
    A.B
    A.compareTo
    A.toString
    A.equals
    A.hashCode

    A.<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>values<!>()
    A.<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>valueOf<!>("B")

    A.B.<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>name<!>
    A.B.<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>ordinal<!>
    A.B.<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>compareTo<!>(A.B)
    A.B.<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>hashCode<!>()

    println(A.B == A.compareTo)

    with(A.B) {
        println(<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>name<!>)
        println(<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>ordinal<!>)
        println(<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>compareTo<!>(A.B))
        println(<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>hashCode<!>())

        println(::<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>name<!>)
        println(::<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>ordinal<!>)
        println(::<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>compareTo<!>)
        println(::<!WRONG_OPERATION_WITH_EXTERNAL_ENUM!>hashCode<!>)
    }
}
