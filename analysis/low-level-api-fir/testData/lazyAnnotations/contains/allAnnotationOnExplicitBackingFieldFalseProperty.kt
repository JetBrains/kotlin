// QUERY: contains: Property

class MyClass {
    @all:Property
    val property: List<Int>
        fie<caret>ld: MutableList<Int> = null!!
}

@Target(AnnotationTarget.PROPERTY)
annotation class Property
