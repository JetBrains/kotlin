// QUERY: contains: Field

class MyClass {
    @all:Field
    val property: List<Int>
        fie<caret>ld: MutableList<Int> = null!!
}

@Target(AnnotationTarget.FIELD)
annotation class Field
