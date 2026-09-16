// RUN_PIPELINE_TILL: FRONTEND
// FILE: test.kt
class MyValue : Value

<!WRONG_MODIFIER_TARGET!>error<!> class MyError
<!WRONG_MODIFIER_TARGET!>error<!> object MyErrorObject : RichError()

fun <T : Value> bounded() { }
fun <T : Value?> boundedNullable() { }

fun test<!DEPRECATED_TYPE_PARAMETER_SYNTAX!><T, A : Any, V : Value, NV : Value?, E : RichError, R : T><!>() {
    bounded<MyValue>()
    bounded<String>()
    bounded<J>()
    bounded<<!UPPER_BOUND_VIOLATED!>MyValue?<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>String?<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>J?<!>>()

    bounded<<!UPPER_BOUND_VIOLATED!>Any<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>Any?<!>>()

    bounded<<!UPPER_BOUND_VIOLATED!>T<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>A<!>>()
    bounded<V>()
    bounded<<!UPPER_BOUND_VIOLATED!>NV<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>E<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>R<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>T & Any<!>>()
    bounded<<!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>V<!> & Any>()
    bounded<NV & Any>()
    bounded<<!UPPER_BOUND_VIOLATED!><!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>E<!> & Any<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>R & Any<!>>()

    bounded<<!UPPER_BOUND_VIOLATED!>MyError<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>MyErrorObject<!>>()

    bounded<<!UPPER_BOUND_VIOLATED!>String | MyError<!>>()
    bounded<<!UPPER_BOUND_VIOLATED!>MyError | MyErrorObject<!>>()

    boundedNullable<MyValue>()
    boundedNullable<String>()
    boundedNullable<J>()
    boundedNullable<MyValue?>()
    boundedNullable<String?>()
    boundedNullable<J?>()

    boundedNullable<<!UPPER_BOUND_VIOLATED!>Any<!>>()
    boundedNullable<<!UPPER_BOUND_VIOLATED!>Any?<!>>()

    boundedNullable<<!UPPER_BOUND_VIOLATED!>T<!>>()
    boundedNullable<<!UPPER_BOUND_VIOLATED!>A<!>>()
    boundedNullable<V>()
    boundedNullable<NV>()
    boundedNullable<<!UPPER_BOUND_VIOLATED!>E<!>>()
    boundedNullable<<!UPPER_BOUND_VIOLATED!>R<!>>()
    boundedNullable<<!UPPER_BOUND_VIOLATED!>T & Any<!>>()
    boundedNullable<<!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>V<!> & Any>()
    boundedNullable<NV & Any>()
    boundedNullable<<!UPPER_BOUND_VIOLATED!><!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>E<!> & Any<!>>()
    boundedNullable<<!UPPER_BOUND_VIOLATED!>R & Any<!>>()

    boundedNullable<<!UPPER_BOUND_VIOLATED!>MyError<!>>()
    boundedNullable<<!UPPER_BOUND_VIOLATED!>MyErrorObject<!>>()

    boundedNullable<<!UPPER_BOUND_VIOLATED!>String | MyError<!>>()
    boundedNullable<<!UPPER_BOUND_VIOLATED!>MyError | MyErrorObject<!>>()
}

fun <T : Value> T.bounded() { }
fun <T : Value?> T.boundedNullable() { }

interface Box<T> {
    val item: T
}

fun testCaptured(
    star: Box<*>,
    any: Box<out Any>,
    value: Box<out Value>,
    cs: Box<out CharSequence>,
    richError: Box<out RichError>,
    myError: Box<out MyError>,
) {
    star.item.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bounded<!>()
    star.item.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boundedNullable<!>()
    any.item.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bounded<!>()
    any.item.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boundedNullable<!>()
    value.item.bounded()
    value.item.boundedNullable()
    cs.item.bounded()
    cs.item.boundedNullable()
    richError.item.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bounded<!>()
    richError.item.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boundedNullable<!>()
    myError.item.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bounded<!>()
    myError.item.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boundedNullable<!>()
}

// FILE: J.java
public class J {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, typeConstraint, typeParameter */
