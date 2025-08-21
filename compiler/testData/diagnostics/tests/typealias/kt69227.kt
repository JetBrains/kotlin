// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface IModel<T1>
data class ModelWithId<T2 : IModel<T3>, T3>(val id: T3)
typealias ModelWithIdAlias<T4, T5> = ModelWithId<T4, T5>

fun main() {
    ModelWithIdAlias<IModel<Int>, Int>(1)
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, interfaceDeclaration, nullableType,
primaryConstructor, propertyDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint,
typeParameter */
