// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

//KT-304: Resolve supertype reference to class anyway

open class Foo() : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Bar<!>() {
}

open class Bar<T>() {
}

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, typeParameter */
