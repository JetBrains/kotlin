// TARGET_BACKEND: JVM
// ISSUE: KT-19845
// WITH_REFLECT

// KT-19845: Incorrectly compiled spread operator in varargs inside annotation argument list

annotation class B(val i: Int)

annotation class A(vararg val bs: B)

@A(B(1), B(2), *arrayOf(B(4), B(5)), B(6))
class AnnotatedClass

fun box(): String {
    val s = buildString {
        // KClass.annotations is JVM-only member
        (AnnotatedClass::class.annotations.single() as A).bs.forEach {
            append(it.i)
        }
    }
    return if (s == "12456") "OK" else s
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, integerLiteral, outProjection,
primaryConstructor, propertyDeclaration, vararg */
