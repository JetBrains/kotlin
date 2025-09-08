// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

import java.io.File

fun test() {
    val dir = File("dir")
    val files = dir.listFiles()?.toList() ?: listOf() // error
    files checkType { _<List<File>>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, flexibleType, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, javaFunction, lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall,
stringLiteral, typeParameter, typeWithExtension */
