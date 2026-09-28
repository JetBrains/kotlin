// RUN_PIPELINE_TILL: CODEGEN
open class PackageTest

class MoreTest() {
    companion object: PackageTest() {

    }

    object Some: PackageTest()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration, primaryConstructor */
