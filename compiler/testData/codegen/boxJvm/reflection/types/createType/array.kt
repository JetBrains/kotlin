// TARGET_BACKEND: JVM
// WITH_REFLECT
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

class K {
    fun arrayInt(): Array<Int> = arrayOf()
    fun arrayIntN(): Array<Int?> = arrayOf()
    fun arrayString(): Array<String> = arrayOf()
    fun arrayArrayInt(): Array<Array<Int>> = arrayOf()
    fun arrayStar(): Array<*> = arrayOf<Any>()
    fun arrayOutInt(): Array<out Int> = arrayOf()
    fun arrayOutNumber(): Array<out Number> = arrayOf()
    fun arrayInInt(): Array<in Int> = arrayOf<Any>()
    fun arrayInNumber(): Array<in Number> = arrayOf<Any>()
    fun arrayInArrayInt(): Array<in Array<Int>> = arrayOf<Any>()
}

fun checkEquals(a: KType, b: KType) {
    assertEquals(a, b)
    assertEquals(b, a)
    assertEquals(a.classifier, b.classifier)
    assertEquals(a.arguments, b.arguments)
    assertEquals(a.javaType, b.javaType)
    assertEquals(a.hashCode(), b.hashCode())
    assertEquals(a.toString(), b.toString())
}

fun check(created: KType, fromTypeOf: KType, fromMember: KType) {
    checkEquals(fromTypeOf, created)
    checkEquals(fromMember, created)
    checkEquals(fromMember, fromTypeOf)
}

fun box(): String {
    check(
        Array::class.createType(listOf(KTypeProjection.invariant(Int::class.createType()))),
        typeOf<Array<Int>>(),
        K::arrayInt.returnType,
    )
    check(
        Array::class.createType(listOf(KTypeProjection.invariant(Int::class.createType(nullable = true)))),
        typeOf<Array<Int?>>(),
        K::arrayIntN.returnType,
    )
    check(
        Array::class.createType(listOf(KTypeProjection.invariant(String::class.createType()))),
        typeOf<Array<String>>(),
        K::arrayString.returnType,
    )
    check(
        Array::class.createType(
            listOf(KTypeProjection.invariant(Array::class.createType(listOf(KTypeProjection.invariant(Int::class.createType())))))
        ),
        typeOf<Array<Array<Int>>>(),
        K::arrayArrayInt.returnType,
    )
    check(
        Array::class.createType(listOf(KTypeProjection.STAR)),
        typeOf<Array<*>>(),
        K::arrayStar.returnType,
    )

    check(
        Array::class.createType(listOf(KTypeProjection.covariant(Int::class.createType()))),
        typeOf<Array<out Int>>(),
        K::arrayOutInt.returnType,
    )
    check(
        Array::class.createType(listOf(KTypeProjection.covariant(Number::class.createType()))),
        typeOf<Array<out Number>>(),
        K::arrayOutNumber.returnType,
    )
    check(
        Array::class.createType(listOf(KTypeProjection.contravariant(Int::class.createType()))),
        typeOf<Array<in Int>>(),
        K::arrayInInt.returnType,
    )
    check(
        Array::class.createType(listOf(KTypeProjection.contravariant(Number::class.createType()))),
        typeOf<Array<in Number>>(),
        K::arrayInNumber.returnType,
    )
    check(
        Array::class.createType(
            listOf(KTypeProjection.contravariant(Array::class.createType(listOf(KTypeProjection.invariant(Int::class.createType())))))
        ),
        typeOf<Array<in Array<Int>>>(),
        K::arrayInArrayInt.returnType,
    )

    assertEquals(typeOf<Array<*>>().classifier, typeOf<Array<in Number>>().classifier)
    assertEquals(Array<Any>::class.java, typeOf<Array<in Number>>().javaType)
    assertEquals(Array<Any>::class.java, K::arrayInNumber.returnType.javaType)
    assertEquals(Array<Number>::class.java, typeOf<Array<out Number>>().javaType)
    assertEquals(Array<Number>::class.java, K::arrayOutNumber.returnType.javaType)

    return "OK"
}
