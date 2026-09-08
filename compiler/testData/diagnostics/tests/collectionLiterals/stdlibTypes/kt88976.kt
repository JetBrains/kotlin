// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-88976

data class Box<T>(val x: T)

fun useList(lst: List<Box<() -> Unit>>) = Unit
fun useListWithParam(lst: List<Box<(Int) -> Unit>>) = Unit

fun a() {
    val x: List<Box<() -> Unit>> = [Box {}]
}

fun b() {
    val x: List<Box<() -> Unit>> = [Box<() -> Unit> {}]
}

fun c() {
    val x: List<Box<() -> Unit>> = listOf(Box {})
}

fun d() {
    val x: List<Box<() -> Unit>> = buildList {
        addAll([Box {}])
    }
}

fun e() {
    val x: List<() -> Unit> = [{}]
}

fun f() {
    useList([Box {}])
    useListWithParam([Box {}])
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, functionalType, lambdaLiteral, localClass,
localProperty, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
