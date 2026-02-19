// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
//KT-1778 Automatically cast error
package kt1778

import checkSubtype

fun main(args : Array<String>) {
    val x = checkSubtype<Any>(args[0])
    if(x is <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.CharSequence<!>) {
        if ("a" == x) x.length else x.length() // OK
        if ("a" == x || "b" == x) x.length else x.length() // <– THEN ERROR
        if ("a" == x && "a" == x) x.length else x.length() // <– ELSE ERROR
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, disjunctionExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression, infix, integerLiteral, intersectionType,
isExpression, javaFunction, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, typeParameter,
typeWithExtension */
