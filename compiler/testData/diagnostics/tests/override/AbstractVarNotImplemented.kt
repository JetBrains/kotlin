// RUN_PIPELINE_TILL: FRONTEND
abstract class A {
    abstract var i: Int
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class B<!>() : A() {
}

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration */
