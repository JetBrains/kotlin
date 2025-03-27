package foo

const val CONSTANT = 5
annotation class Anno(val s: String)

class MyClass {
    companion object {
        const val NESTED_CONSTANT = 0
    }

    annotation class NestedAnnotation(val i: Int)

    @Anno("str" + CONSTANT) <expr>@NestedAnnotation(NESTED_CONSTANT)</expr>
}
