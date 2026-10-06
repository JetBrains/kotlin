package test

class C {
    fun nonNullReturn(): Unknown = TODO()
    fun nullableReturn(): Unknown? = TODO()

    fun params(nonNull: Unknown, nullable: Unknown?) {}

    val nonNullProp: Unknown = TODO()
    val nullableProp: Unknown? = null

    fun typeArgs(): List<Unknown?> = TODO()
    fun genericError(): Unknown<String>? = TODO()
}

// COMPILATION_ERRORS
