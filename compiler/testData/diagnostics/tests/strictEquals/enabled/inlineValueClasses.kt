// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// MODULE: m1

@JvmInline
value class Regular1(val x: Int)

interface Interface {
    val x: CharSequence
    override fun equals(@EqualityBound(Interface::class) other: Any?): Boolean
}

@JvmInline
value class Inherited1(override val x: String) : Interface

@JvmInline
value class Generic1<T : CharSequence>(val x: T)

// MODULE: m2(m1)

@JvmInline
value class Regular2(val x: Int)

@JvmInline
value class Inherited2(override val x: String) : Interface

@JvmInline
value class Generic2<T : CharSequence>(val x: T)

fun useSite(
    r1: Regular1,
    r2: Regular2,
    ei1: Inherited1,
    ei2: Inherited2,
    gen1: Generic1<String>,
    gen2: Generic2<String>,
    nany: Any?,
) {
    if (r1 == nany) {
        nany.x
    }
    if (r2 == nany) {
        nany.x
    }
    if (ei1 == nany) {
        nany.x.length
        nany.x.plus("!")
    }
    if (ei2 == nany) {
        nany.x.length
        nany.x.plus("!")
    }
    if (gen1 == nany) {
        nany.x.length
        nany.x.<!NONE_APPLICABLE!>plus<!>("!")
    }
    if (gen2 == nany) {
        nany.x.length
        nany.x.<!NONE_APPLICABLE!>plus<!>("!")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration,
ifExpression, interfaceDeclaration, nullableType, operator, override, primaryConstructor,
propertyDeclaration, smartcast, stringLiteral, value */
