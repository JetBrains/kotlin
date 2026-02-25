// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// ISSUE: KT-54978
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +ContextReceivers, -ContextParameters

// Case 1: Parameters and local variables
fun f1(x: Int) {
    val y = 5
    val s = "hello"

    x<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int><!>
    x<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String, String><!>
    y<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int><!>
    y<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String, Int><!>
    s<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String><!>
    s<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int, String><!>
}

// Case 2: Simple property
val property: Int = 10

fun f2() {
    property<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String><!>
}

// Case 3: Simple property with getter
val property2: Int
    get() = 10

fun f3() {
    property2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String><!>
}

// Case 4: Property with extension and/or context receiver
interface Context<A>
class ContextImpl<A> : Context<A>
class Receiver<A>

val <A> Receiver<A>.hello1: String
    get() = "hello 1"

context(Context<A>)
val <A> hello2: String
    get() = "hello 2"

context(Context<B>)
val <A, B> Receiver<A>.hello3: String
    get() = "hello 3"

operator fun <A, B> String.invoke(): String = "world"

fun f4() {
    val receiver = Receiver<Int>()

    receiver.hello1
    receiver.hello1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int><!>
    receiver.<!TYPE_ARGUMENTS_NOT_ALLOWED("on implicit invoke call")!>hello1<!><Int, String>()
    receiver.hello1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String><!>
    receiver.hello1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int, String><!>
    receiver.hello1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int, String, String><!>

    with (ContextImpl<String>()) {
        hello2
        hello2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String><!>
        <!TYPE_ARGUMENTS_NOT_ALLOWED("on implicit invoke call")!>hello2<!><String, Int>()
        hello2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int><!>
        hello2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String, Int><!>
        hello2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String, Int, Int><!>

        receiver.hello3
        receiver.hello3<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int, String><!>
        receiver.<!TYPE_ARGUMENTS_NOT_ALLOWED("on implicit invoke call")!>hello3<!><Int, String>()
        receiver.hello3<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String, Int><!>
        receiver.hello3<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int><!>
        receiver.hello3<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int, String, String><!>
    }
}

// Case 5: Property with receiver and reified type parameter
inline val <reified A> Receiver<A>.helloReified: String
    get() = "hello"

fun f5() {
    val receiver = Receiver<Int>()
    receiver.helloReified
    receiver.helloReified<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><Int><!>
    receiver.helloReified<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS("Property")!><String><!>
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
interfaceDeclaration, lambdaLiteral, localProperty, nullableType, operator, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, reified, stringLiteral, typeParameter */
