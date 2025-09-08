// RUN_PIPELINE_TILL: FRONTEND
package myPack

annotation class Anno(val number: Int)

fun topLevelFun() {
    class LocalClass {
        val @receiver:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.prop<!>) Int.prop get() = 22
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetReceiver, classDeclaration, functionDeclaration,
getter, integerLiteral, localClass, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver */
