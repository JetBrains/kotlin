// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package a

interface Persistent
interface PersistentFactory<T>

class Relation<Source: Persistent, Target: Persistent>(
        val sources: PersistentFactory<Source>,
        val targets: PersistentFactory<Target>
) {
    fun opposite() = Relation(targets, sources)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, primaryConstructor,
propertyDeclaration, typeConstraint, typeParameter */
