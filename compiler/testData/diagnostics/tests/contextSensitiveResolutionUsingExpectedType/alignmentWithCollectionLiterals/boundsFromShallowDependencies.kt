// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232
// FIR_DUMP

sealed class MySealed {
    object Left : MySealed()
    object Right : MySealed()
}

enum class MyEnum { X, Y }

interface MyInterface

sealed class MySecondSealed : MyInterface {
    object First : MySecondSealed()
    object Second : MySecondSealed()
}

fun <T> select(x: T, y: T): T = x
fun <T> select3(x: T, y: T, z: T): T = x
fun <S> id(x: S): S = x
fun <T : MySealed> takeA(x: T): T = x
fun <T : MyInterface> takeI(x: T, y: T): T = x

fun test() {
    // S <: T (from `id(Right)` passed to `select`), MySealed.Left <: T, T <: MySealed: the bounds of T are reached through S
    val a: MySealed = select(MySealed.Left, id(Right))
    val b: MySealed = select(id(Right), MySealed.Left)
    val c: MySealed = select(MySealed.Left, id(id(Right)))
    val d: MySealed = select(id(MySealed.Left), id(Right))

    // S <: T, T <: MySealed (declared bound)
    val e = takeA(id(Right))

    // The upper bound of T is an interface, which has no `Second` name
    val f: MyInterface = select(MySecondSealed.First, id(Second))
    val g = takeI(id(Second), MySecondSealed.First)

    // T has several lower bounds, and only one of the classes has `Second` name
    val h = select3(MySecondSealed.First, MyEnum.X, id(Second))
    val i = select3(MySecondSealed.First, "", id(Second))
    val j = select3(id(Second), 1, MySecondSealed.First)
    val k: Any = select3(id(Second), MySecondSealed.First, MyEnum.X)
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, integerLiteral,
interfaceDeclaration, localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration, sealed,
stringLiteral, typeConstraint, typeParameter */
