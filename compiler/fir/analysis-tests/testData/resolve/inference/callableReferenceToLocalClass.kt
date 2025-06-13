// RUN_PIPELINE_TILL: BACKEND
fun <T, R> List<T>.myMap(block: (T) -> R): List<R> = null!!

fun test_1() {
    class Data(val x: Int)
    val datas: List<Data> = null!!
    val xs = datas.myMap(Data::x)
}

/* GENERATED_FIR_TAGS: callableReference, checkNotNullCall, classDeclaration, funWithExtensionReceiver,
functionDeclaration, functionalType, localClass, localProperty, nullableType, primaryConstructor, propertyDeclaration,
typeParameter */
