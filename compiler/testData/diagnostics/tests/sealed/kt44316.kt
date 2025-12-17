// RUN_PIPELINE_TILL: BACKEND
// KT-44316

sealed class Base
class Derived : Base()

class Test<out V>(val x: Base) {
    private val y = when (x) {
        is Derived -> null
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, isExpression, nullableType, out, primaryConstructor, propertyDeclaration,
sealed, typeParameter, whenExpression, whenWithSubject */
