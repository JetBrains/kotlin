open class A {
    val pr<caret>op: Any
        @Anno("backing field: $constant") field: @Anno("type: $constant") MyList<@Anno("nested type: $constant") MyList<@Anno("nested nested type: $constant") Int>> = null!!
}

interface MyList<T>

@Target(AnnotationTarget.TYPE, AnnotationTarget.FIELD)
annotation class Anno(val position: String)
const val constant = 0