package foo

@Target(AnnotationTarget.TYPE, AnnotationTarget.LOCAL_VARIABLE)
annotation class Anno(val position: String)
const val constant = 0

fun explicitType(): @Anno("return type: $constant") List<@Anno("nested return type: $constant") Collection<@Anno("nested nested return type: $constant") String>> = 0

fun resolveMe() {
    <expr>
    @Anno("property $secondConstant")
    val localProperty = explicitType()
    </expr>
}

const val secondConstant = "str"
