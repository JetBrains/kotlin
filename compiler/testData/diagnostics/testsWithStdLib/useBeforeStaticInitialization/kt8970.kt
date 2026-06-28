// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
abstract class A(val x : Any?)
<!POSSIBLE_INITIALIZATION_DEADLOCK!>object B<!> : A(<!ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE!>C<!>)
<!POSSIBLE_INITIALIZATION_DEADLOCK!>object C<!> : A(<!ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE!>B<!>)

abstract class Base(val x: Any?)

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class C1 {
    companion object : Base(<!ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE!>C2<!>)
}<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class C2 {
    companion object : Base(<!ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE!>C1<!>)
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nullableType, objectDeclaration, primaryConstructor,
propertyDeclaration */
