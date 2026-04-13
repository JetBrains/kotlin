// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-64982
// FIR_DUMP

class Outer<T> {
    companion object

    class Nested<S> {
        companion object
    }

    inner class Inner<R> {
        companion <!NESTED_CLASS_NOT_ALLOWED!>object<!>
    }

    object Obj
}

val test = Outer<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><String><!>

val test2 = Outer.Nested<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><String><!>

val test3 = Outer<Int>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Inner<!><!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Double><!>

val test4 = Outer<Int>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Obj<!>

val test5 = Outer

val test6 = Outer.Nested

val test7 = Outer.Inner

val test8 = Outer.Obj

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, inner, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, typeParameter */
