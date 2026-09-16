// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232
// FIR_DUMP

sealed class MySealed {
    object Left : MySealed()
    object Right : MySealed()
}

interface MyInterface

sealed class MySecondSealed : MyInterface {
    object First : MySecondSealed()
    object Second : MySecondSealed()
}

fun <T> select(x: T, y: T): T = x
fun <T> select3(x: T, y: T, z: T): T = x
fun <T> selectWithBound(x: T, y: T, upper: Inv<T>): T = x
fun <T : MyInterface> selectI(x: T, y: T): T = x

class Inv<T>

sealed class MySealedGeneric<T> {
    object StringInheritor : MySealedGeneric<String>()
}

fun <T> takeF(x: MySealedGeneric<T>) = Unit
fun <T> takeFAndT(x: MySealedGeneric<T>, y: T) = Unit

enum class MyEnum { X, Y }

fun nullableEnum(): MyEnum? = Y

fun test(b: Boolean, invA: Inv<MySealed>) {
    val p: MySealed = select(MySealed.Left, Right)
    val q: MySealed = select(Right, MySealed.Left)
    val r: MySealed = select(Left, Right)
    val s = select(MySealed.Left, Right)

    // The same through the type variable of an `if` expression
    val p2: MySealed = if (b) MySealed.Left else Right
    val q2: MySealed = if (b) Right else MySealed.Left
    val r2: MySealed = if (b) Left else Right
    val s2 = if (b) MySealed.Left else Right

    // The declared upper bound is an interface implemented by the sealed class, so only the lower bound gives the class
    val i1: MyInterface = select(MySecondSealed.First, Second)
    val i2 = selectI(Second, MySecondSealed.First)
    val i3: MyInterface = if (b) MySecondSealed.First else Second

    // One argument gives the class, one is irrelevant, and one is resolved context-sensitively
    val t1 = select3(MySealed.Left, 1, Right)
    val t2 = select3("", MyEnum.X, Y)
    val t3: Any = select3(Right, MySealed.Left, "")
    val t4 = select3(MySecondSealed.First, 1, Second)

    val t: MySealed = selectWithBound(MySealed.Left, Right, invA)
    val u = selectWithBound(Right, Right, invA)

    takeF(StringInheritor)
    takeFAndT(StringInheritor, "")

    val v: MyEnum = if (b) MyEnum.X else Y
    val w: MyEnum = nullableEnum() ?: X
    val x = nullableEnum() ?: X
    val y: MyEnum? = if (b) Y else null
}

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, enumDeclaration, enumEntry, functionDeclaration, ifExpression,
integerLiteral, interfaceDeclaration, intersectionType, localProperty, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, sealed, stringLiteral, typeConstraint, typeParameter */
