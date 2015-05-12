package test.innerTest

fun <T1> T1.func(a: T1): T1 = a
val <T2> T2.prop: T2? = null
var propVar: Any? = null

interface Trait<T3> {
    fun traitFunc(): Unit {}
    val traitProp: Any?
}

abstract class Class<T4> : Trait<Any> {
    fun classFunc(): Unit {}
    val classProp: Any? = null

    companion object {
        fun classObjFunc(): Unit {}
        val classObjProp: Any? = null
    }
}

object Object {
    fun objFunc(): Unit {}
    val objProp: Any? = null
}

interface Outer {
    interface NestedTrait {
        fun nestedTraitFun() {}
    }

    abstract class NestedClass {
        companion object {
            fun nestedClassObjFunc(): Unit {}
        }
    }

    object NestedObject {
        fun nestedFunc(): Unit {}
    }
}