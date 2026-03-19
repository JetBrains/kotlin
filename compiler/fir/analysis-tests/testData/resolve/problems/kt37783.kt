// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-37783

// KT-37783: MPP Java Interop: Extra functions declared in actual super classes can't be resolved in Java for a common child class

<!NOT_A_MULTIPLATFORM_COMPILATION!>expect<!> open class <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>MPPSuper<!>()

class CommonChild : MPPSuper()

<!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> open class <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>MPPSuper<!> <!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> constructor() {
    fun hello(): String = "hi" // extra function on Jvm
}

fun test() {
    MPPSuper().<!UNRESOLVED_REFERENCE!>hello<!>() // ok
    CommonChild().<!UNRESOLVED_REFERENCE!>hello<!>() // should be ok - method inherited from actual superclass
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, primaryConstructor, stringLiteral */
