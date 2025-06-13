// RUN_PIPELINE_TILL: FRONTEND
package myPack

annotation class Anno(val number: Int)

fun topLevelFun() {
    class LocalClass {
        fun @receiver:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.function()<!>) Int.function() = 1
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetReceiver, classDeclaration,
funWithExtensionReceiver, functionDeclaration, integerLiteral, localClass, primaryConstructor, propertyDeclaration */
