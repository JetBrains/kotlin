// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE
// WITH_STDLIB

class FqName

object FqNames {
    val map: FqName = TODO()
}

class ClassId

fun topLevel(x: FqName): ClassId = TODO()

fun main() {
    buildList {
        add(topLevel(
            // NB: When attempting to resolve `map` as CSR alternative we stumble upon `kotlin.collections.map`
            FqNames.map
        ))
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, funWithExtensionReceiver, functionDeclaration,
lambdaLiteral, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
