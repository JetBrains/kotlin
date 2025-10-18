// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-81254

interface MyFlow<E>

fun <U> myCombine(
    flows: MyFlow<out U>,
    transform: (MutableCollection<U>) -> Unit
) {}

fun <U> myCombine2(
    flows: MyFlow<out U>,
    transform: (MutableCollection<U>) -> Unit
) {}

fun <F> materializeFlow(): MyFlow<F> = TODO()
fun <F2> materializeFlow2(): MyFlow<F2> = TODO()
fun <F3> materializeFlow3(): MyFlow<F3> = TODO()
fun <F4> materializeFlow4(): MyFlow<F4> = TODO()

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("myMap1")
fun <T1> MutableCollection<out T1>.myMap(transform: (T1) -> String) {}

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("myMap2")
fun <T2> MutableCollection<out T2>.myMap(transform: (T2) -> Int) {}

class MyPair<R1, R2>

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("myMapWithArg1")
fun <T1> MutableCollection<out T1>.myMapWithArg(v1: MyFlow<out T1>, v2: MyFlow<in T1>, transform: (T1) -> MyPair<String, in T1>) {}

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("myMapWithArg2")
fun <T2> MutableCollection<out T2>.myMapWithArg(v1: MyFlow<out T2>, v2: MyFlow<in T2>, transform: (T2) -> MyPair<Int, in T2>) {}


fun main() {
    // MyFlow<F> <: MyFlow<out U> => F <: U
    myCombine(materializeFlow()) { w /*: MutableCollection<U> */ ->
        // String <: U
        w.add("")
        // MutableCollection<U> <: MutableCollection<out Ti> => U <: Ti (for both i=1..2)
        //  => F <: Ti
        //  => Ti := CST(U, F, String) := String
        w.myMap { "" }
    }

    // MyFlow<F> <: MyFlow<out U> => F <: U
    myCombine(materializeFlow()) { w /*: MutableCollection<U> */ ->
        // MutableCollection<U> <: MutableCollection<out Ti> => U <: Ti (for both i=1..2)
        //  => F <: Ti
        //  => Ti has only U and F lower bounds => we can't fix the type variables => can't choose the overload
        w.<!OVERLOAD_RESOLUTION_AMBIGUITY!>myMap<!> { "" }
        w.add("")
    }

    // F2 <: U
    myCombine2(materializeFlow2()) { w /*: MutableCollection<U> */ ->
        // U <: Ti
        //  => F2 <: Ti
        // Flow<F3> <: Flow<out Ti> => F3 <: Ti
        // Flow<F4> <: Flow<in Ti> => Ti <: F4
        // T1 type varible lower and upper constraints and while we cannot compute CST(U, F2, F3) we can choose the upper one constraint
        // Thus, semifixng Ti := F4 and then choosing the first overload as it matches the lambda return value
        // After that, having MyPair<String, Int> <: MyPair<String, in T1>  => T1 <: Int from which we _actually_ fix both U and T1 to Int
        w.myMapWithArg(materializeFlow3(), materializeFlow4()) { MyPair<String, Int>() }
        w.add(123)
    }

    myCombine2(materializeFlow2()) { w /*: MutableCollection<U> */ ->
        w.myMapWithArg(materializeFlow3(), materializeFlow4()) { MyPair<String, Int>() }
        // The same as in the case above, but we don't allow type-unsafe operation
        w.add(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    }
}

/* GENERATED_FIR_TAGS: crossinline, funWithExtensionReceiver, functionDeclaration, functionalType, inline,
interfaceDeclaration, lambdaLiteral, nullableType, propertyDeclaration, reified, suspend, typeParameter, vararg */
