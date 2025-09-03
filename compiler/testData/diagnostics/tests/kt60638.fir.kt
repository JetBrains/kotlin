// RUN_PIPELINE_TILL: FRONTEND
package usage

class MyType
class MyClass
val MyClass.isInterface get() = 4

fun usage(type: MyType) {
    type.<!FUNCTION_EXPECTED, IMPLICIT_PROPERTY_TYPE_ON_INVOKE_LIKE_CALL, IMPLICIT_PROPERTY_TYPE_ON_INVOKE_LIKE_CALL!>isInterface<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, propertyDeclaration,
propertyWithExtensionReceiver */
