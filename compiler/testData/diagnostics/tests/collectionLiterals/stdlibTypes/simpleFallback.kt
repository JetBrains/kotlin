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
    val _: Any = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _: Any = [1, 2, 3]

    val _: Collection<*> = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _: Collection<*> = [1, 2, 3]

    val _: Collection<Int> = []
    val _: Collection<Int> = [1, 2, 3]
    val _: Collection<Long> = [1, 2, 3]
    val _: Collection<String> = [<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>]

    val _: Iterable<*> = [1, 2, 3]
    val _: Iterable<Number> = []
    val _: Iterable<Number> = [1, 2, 3]

    val _: Any? = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _: Any? = [1, 2, 3]

    takeAny(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    takeAny([1, 2, 3])
    <!CANNOT_INFER_PARAMETER_TYPE!>takeIterable<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    takeIterable<Int>([])
    takeIterable<String>([<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>])
    takeIterable([1, 2, 3])
    takeIterable([1, 2L, 3])
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableCollection<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    takeNullableCollection([1, 2, 3])

    Utils.takeAny(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    Utils.takeAny([1, 2, 3])

    println([])
    println([1, 2, 3])

    mutableListOf(1, 2, 3).addAll([4, 5, 6])
}

class MyCollection

fun couldntFallback() {
    val _: CharSequence = <!UNRESOLVED_COLLECTION_LITERAL!>['a', 'b', 'c']<!>
    val _: CharSequence = <!UNRESOLVED_COLLECTION_LITERAL!>[]<!>
    val _: CharSequence? = <!UNRESOLVED_COLLECTION_LITERAL!>['a', 'b', 'c']<!>

    val _: Int = <!UNRESOLVED_COLLECTION_LITERAL!>[]<!>
    val _: Int? = <!UNRESOLVED_COLLECTION_LITERAL!>[1, 2, 3]<!>

    val _: MutableCollection<*> = <!UNRESOLVED_COLLECTION_LITERAL!>[1, 2, 3]<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMutableCollection<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[1, 2, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMutableCollection<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)

    val _: MyCollection = <!UNRESOLVED_COLLECTION_LITERAL!>[]<!>
}

fun noBounds() {
    <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    [1, 2, 3]

    val _ = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _ = [1, 2, 3]

    [1, 2, 3].forEach {
        println(it)
    }

    for (i in [1, null, 3]) {
        println(i)
    }

    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>with<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>) <!CANNOT_INFER_PARAMETER_TYPE!>{ }<!>
    with([1, 2, 3]) { }

    [1, 2, 3].run {
        get(1)
    }

    [1, 2, 3] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]

    [0][0]

    take([1, 2, 3])
    <!CANNOT_INFER_PARAMETER_TYPE!>take<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    takeDnn([1, 2, 3])
    Utils.take([1, 2, 3])
}

fun <T> defaultParameters(
    x: Any = [1, 2, 3],
    y: Collection<T> = [],
    z: MutableCollection<T> = <!UNRESOLVED_COLLECTION_LITERAL!>[]<!>,
) {
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, dnnType, forLoop, functionDeclaration, integerLiteral,
javaFunction, lambdaLiteral, localProperty, nullableType, propertyDeclaration, starProjection, typeParameter,
unnamedLocalVariable */
