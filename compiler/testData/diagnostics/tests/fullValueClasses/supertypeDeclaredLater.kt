// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// FILE: a.kt

value class Sub(val a: Int, val b: Long) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>SBase<!>()

value class OtherSub(val a: Int) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>OtherBase<!>(a)

// FILE: b.kt

<!VALUE_CLASS_NOT_FINAL!>sealed<!> <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class SBase

<!VALUE_CLASS_NOT_FINAL!>abstract<!> value class OtherBase(x: Int)

value class SameFileSub(override val a: String) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>SameFileBase<!>()

<!VALUE_CLASS_NOT_FINAL!>abstract<!> <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class SameFileBase {
    abstract val a: String
}

/* GENERATED_FIR_TAGS: classDeclaration, override, primaryConstructor, propertyDeclaration, sealed, value */
