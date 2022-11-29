// ISSUE: KT-54978

// Case 1: Parameters and local variables
fun f1(x: Int) {
    val y = 5
    val s = "hello"

    <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>x<!><Int>
    <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>x<!><String, String>
    <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>y<!><Int>
    <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>y<!><String, Int>
    <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>s<!><String>
    <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>s<!><Int, String>
}

// Case 2: Simple property
val property: Int = 10

fun f2() {
    <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>property<!><String>
}

// Case 3: Simple property with getter
val property2: Int
    get() = 10

fun f3() {
    <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>property2<!><String>
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
    receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello1<!><Int>
    receiver.hello1<Int, String>() // legal `String.invoke` call
    receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello1<!><String>
    receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello1<!><Int, String>
    receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello1<!><Int, String, String>

    with (ContextImpl<String>()) {
        hello2
        <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello2<!><String>
        hello2<String, Int>() // legal `String.invoke` call
        <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello2<!><Int>
        <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello2<!><String, Int>
        <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello2<!><String, Int, Int>

        receiver.hello3
        receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello3<!><Int, String>
        receiver.hello3<Int, String>() // legal `String.invoke` call
        receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello3<!><String, Int>
        receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello3<!><Int>
        receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>hello3<!><Int, String, String>
    }
}

// Case 5: Property with receiver and reified type parameter
inline val <reified A> Receiver<A>.helloReified: String
    get() = "hello"

fun f5() {
    val receiver = Receiver<Int>()
    receiver.helloReified
    receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>helloReified<!><Int>
    receiver.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>helloReified<!><String>
}
