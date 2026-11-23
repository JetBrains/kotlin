// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88675
// WITH_STDLIB

interface Artifact
class MyArtifact(val value: String) : Artifact

operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

class Foo {
    val strings: List<String> = listOf()
    val artifacts <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> <!ARGUMENT_TYPE_MISMATCH!>if (true) strings.map { MyArtifact(it) } else emptyList()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, ifExpression,
interfaceDeclaration, lambdaLiteral, nullableType, operator, primaryConstructor, propertyDeclaration, propertyDelegate,
starProjection, thisExpression, typeConstraint, typeParameter */
