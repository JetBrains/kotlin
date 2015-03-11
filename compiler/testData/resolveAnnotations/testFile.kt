[file: ANNOTATION]

package test

import test.MyEnum.*

ANNOTATION class MyClass [ANNOTATION]([ANNOTATION] param: Int, [ANNOTATION] val consProp: Int) {
    ANNOTATION default object {
    }

    ANNOTATION var prop: Int = 1
        [ANNOTATION] get
        [ANNOTATION] set([ANNOTATION] param) = $prop = param
    ANNOTATION fun foo([ANNOTATION] param: Int) {
        [ANNOTATION] class LocalClass { }

        [ANNOTATION] object LocalObject { }

        [ANNOTATION] fun localFun() {}

        [ANNOTATION] var localVar: Int = 1
    }

    ANNOTATION class InnerClass {
    }

}

ANNOTATION object MyObject {
}

ANNOTATION var topProp: Int = 1
    [ANNOTATION] get
    [ANNOTATION] set([ANNOTATION] param) = $topProp = param

ANNOTATION fun topFoo([ANNOTATION] param: Int) {
}

val funLiteral = {([ANNOTATION] a: Int) -> a }


annotation class AnnString(a: String)
annotation class AnnInt(a: Int)
annotation class AnnEnum(a: MyEnum)
annotation class AnnIntArray(a: IntArray)
annotation class AnnIntVararg(vararg a: Int)
annotation class AnnStringVararg(vararg a: String)
annotation class AnnStringArray(a: Array<String>)
annotation class AnnArrayOfEnum(a: Array<MyEnum>)
annotation class AnnAnn(a: AnnInt)
annotation class AnnClass(a: Class<*>)

enum class MyEnum {
  A
}
