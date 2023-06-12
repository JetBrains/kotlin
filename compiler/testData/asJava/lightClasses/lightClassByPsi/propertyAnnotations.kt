// !GENERATE_PROPERTY_ANNOTATIONS_METHODS

annotation class Anno(val p: String = "")


@Deprecated("deprecated")
val deprecated = 0

@Volatile
@Transient
var jvmFlags = 0

class C {
    companion object {
        @Anno("x")
        val x = 1

        @JvmStatic
        @Anno("y")
        val y = 2
    }
}

@Anno("propery")
val <T: Any> @receiver:Anno("receiver") List<T>.extensionProperty: Int
    get() = 0

@Anno("nullable")
val nullable: String? = null

@Anno("nonNullable")
val nonNullable: String = ""

open class O {
    @Anno("private")
    private val privateProperty: Int get() = 1

    @Anno("protected")
    protected val protectedProperty = 1
}