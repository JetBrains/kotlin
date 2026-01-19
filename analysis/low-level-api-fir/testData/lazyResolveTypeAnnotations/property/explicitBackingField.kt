open class A {
    val pr<caret>op: Any
        field: @Ann Int = 1
}

@Target(AnnotationTarget.TYPE)
annotation class Ann