// QUERY: get: Field

class MyClass {
    @all:Another
    val property: List<Int>
        fie<caret>ld: MutableList<Int> = null!!
}

@Target(AnnotationTarget.FIELD)
annotation class Field

@Target(AnnotationTarget.FIELD)
annotation class Another
