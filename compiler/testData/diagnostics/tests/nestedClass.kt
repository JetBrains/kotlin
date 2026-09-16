// RUN_PIPELINE_TILL: CODEGEN
abstract class Base(val s: String)

class Outer {
    class Derived(s: String) : Base(s)

    object Obj : Base("")
}

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral */
