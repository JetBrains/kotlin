// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
abstract class Base {
    abstract fun <T> foo(list: List<T>) where T : Number, T : Comparable<T>
}

class Derived : Base() {
    override fun <T> foo(list: List<T>) where T : Number, T : Comparable<T> {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, override, typeConstraint, typeParameter */
