// RUN_PIPELINE_TILL: FRONTEND
fun main() {
    Clazz.<!ENUM_CLASS_CONSTRUCTOR_CALL!><!INVISIBLE_REFERENCE!>InnerEnum<!>()<!>
}

class Clazz {
    enum class InnerEnum { V1 }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, nestedClass */
