// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK
// IDE_MODE
// ISSUE: KT-85267

object Properties {
    val property: String? get() = null
}

fun <T> id(t: T): T = TODO()

val <F> F.myProp: String get() = ""

fun test() {
    if (Properties.property != null) {
        Properties.property?.length

        val x: String? = Properties.property

        id(Properties.property)
        Properties.property.myProp
        Properties.property!!
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, equalityExpression, funWithExtensionReceiver, functionDeclaration, getter,
ifExpression, localProperty, nullableType, objectDeclaration, propertyDeclaration, safeCall, smartcast, typeParameter */
