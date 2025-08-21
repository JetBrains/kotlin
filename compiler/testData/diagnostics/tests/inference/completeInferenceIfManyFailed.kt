// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

package d

import checkSubtype

fun <T: Any> joinT(x: Int, vararg a: T): T? {
    return null
}

fun <T: Any> joinT(x: Comparable<*>, y: T): T? {
    return null
}

fun test() {
    val x2 = joinT(<!TYPE_MISMATCH!>Unit<!>, "2")
    checkSubtype<String?>(x2)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
localProperty, nullableType, propertyDeclaration, starProjection, stringLiteral, typeConstraint, typeParameter,
typeWithExtension, vararg */
