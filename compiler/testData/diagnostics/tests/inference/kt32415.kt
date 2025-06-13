// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

abstract class TestType<V: Any> {
    open inner class Inner(val item: V)
}

class Derived: TestType<Long>() {
    inner class DerivedInner(item: Long): Inner(item)
}

/* GENERATED_FIR_TAGS: classDeclaration, inner, primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
