package test

import java.lang.annotation.RetentionPolicy

// CORRECT
annotation class Ann1(val p1: Int,
                      val p2: Byte,
                      val p3: Short,
                      val p4: Long,
                      val p5: Double,
                      val p6: Float,
                      val p7: Char,
                      val p8: Boolean)

annotation class Ann2(val p1: String)
annotation class Ann3(val p1: Ann1)
annotation class Ann4(val p1: IntArray,
                      val p2: ByteArray,
                      val p3: ShortArray,
                      val p4: LongArray,
                      val p5: DoubleArray,
                      val p6: FloatArray,
                      val p7: CharArray,
                      val p8: BooleanArray)

annotation class Ann5(val p1: MyEnum)

annotation class Ann6(val p: Class<*>)
annotation class Ann7(val p: RetentionPolicy)

annotation class Ann8(val p1: Array<String>,
                      val p2: Array<Class<*>>,
                      val p3: Array<MyEnum>,
                      val p4: Array<Ann1>)

annotation class Ann9(
        val error: Unresolved = <!UNRESOLVED_REFERENCE!>Unresolved<!>.<!UNRESOLVED_REFERENCE!>VALUE<!>
)


// INCORRECT
annotation class InAnn1(val p1: Int?,
                        val p3: Short?,
                        val p4: Long?,
                        val p5: Double?,
                        val p6: Float?,
                        val p7: Char?,
                        val p8: Boolean?)

annotation class InAnn4(val p1: Array<Int>,
                        val p2: Array<Int>?)

annotation class InAnn6(val p:  Class<*>?)
annotation class InAnn7(val p:  RetentionPolicy?)
annotation class InAnn8(val p1: Array<Int>,
                        val p2: Array<Int?>,
                        val p3: Array<MyClass>,
                        val p4: Array<IntArray>)

annotation class InAnn9(val p: MyClass)

annotation class InAnn10(val p1: String?)
annotation class InAnn11(val p1: Ann1?)
annotation class InAnn12(val p1: MyEnum?)

annotation class InAnn13(vararg val p1: String,
                        vararg val p2: Class<*>,
                        vararg val p3: MyEnum,
                        vararg val p4: Ann1,
                        vararg val p5: Int)

enum class MyEnum {
    A
}

class MyClass
