// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals, +UnnamedLocalVariables

// FILE: Utils.java

public class Utils {
    public static void takeAny(Object it) {
    }

    public static <T> void take(T t) {
    }
}

// FILE: tests.kt

fun <T> take(t: T) { }
fun <T> takeDnn(t: T & Any) { }
fun takeAny(a: Any) { }
fun <T> takeIterable(a: Iterable<T>) { }
fun <T> takeNullableCollection(a: Collection<T>?) { }
fun <T> takeMutableCollection(a: MutableCollection<T>) { }

fun couldFallback() {
    val _: Any = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val _: Any = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    val _: Collection<*> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val _: Collection<*> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    val _: Collection<Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val _: Collection<Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
    val _: Collection<Long> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
    val _: Collection<String> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    val _: Iterable<*> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
    val _: Iterable<Number> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val _: Iterable<Number> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    val _: Any? = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val _: Any? = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    takeAny(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    takeAny(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeIterable<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    takeIterable<Int>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    takeIterable<String>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeIterable<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeIterable<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2L, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableCollection<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableCollection<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)

    Utils.takeAny(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    Utils.takeAny(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)

    mutableListOf(1, 2, 3).addAll(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[4, 5, 6]<!>)
}

class MyCollection

fun couldntFallback() {
    val _: CharSequence = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>['a', 'b', 'c']<!>
    val _: CharSequence = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val _: CharSequence? = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>['a', 'b', 'c']<!>

    val _: Int = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val _: Int? = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    val _: MutableCollection<*> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMutableCollection<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMutableCollection<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)

    val _: MyCollection = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
}

fun noBounds() {
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    val _ = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val _ = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>.forEach {
        println(<!UNRESOLVED_REFERENCE!>it<!>)
    }

    for (i in <!ITERATOR_AMBIGUITY, UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, null, 3]<!>) {
        println(i)
    }

    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>with<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>) <!CANNOT_INFER_PARAMETER_TYPE!>{ }<!>
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>with<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>) <!CANNOT_INFER_PARAMETER_TYPE!>{ }<!>

    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>.<!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>run<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        <!CANNOT_INFER_PARAMETER_TYPE!>get<!>(1)
    }<!>

    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!> + <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[4, 5, 6]<!>

    <!CANNOT_INFER_PARAMETER_TYPE!><!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[0]<!>[0]<!>

    <!CANNOT_INFER_PARAMETER_TYPE!>take<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>take<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeDnn<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    Utils.<!CANNOT_INFER_PARAMETER_TYPE!>take<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
}

fun <T> defaultParameters(
    x: Any = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>,
    y: Collection<T> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>,
    z: MutableCollection<T> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>,
) {
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, dnnType, forLoop, functionDeclaration, integerLiteral,
javaFunction, lambdaLiteral, localProperty, nullableType, propertyDeclaration, starProjection, typeParameter,
unnamedLocalVariable */
