// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
abstract class SdkType(val name: String?)

class KotlinSdkType : SdkType(NAME) {
    companion object {
        @JvmField
        val INSTANCE = KotlinSdkType()

        const val NAME = "KotlinSDK"
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, const, nullableType, objectDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
