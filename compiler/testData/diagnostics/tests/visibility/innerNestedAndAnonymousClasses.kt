// KT-49992

import kotlin.reflect.KFunction0

open class A {
    private val x: String? = null

    fun test0() {
        x
        this.x
    }

    open class Nested : A() {
        private val y: String? = null

        fun test1(): String? = <!INVISIBLE_MEMBER!>x<!>
        fun test2(): String? = this.<!INVISIBLE_MEMBER!>x<!>

        class NestedInNested : Nested() {
            fun test20(): String? = <!INVISIBLE_MEMBER!>y<!>
            fun test21(): String? = this.<!INVISIBLE_MEMBER!>y<!>
        }

        inner class InnerInNested : Nested() {
            fun test23(): String? = y
            fun test24(): String? = this.<!INVISIBLE_MEMBER!>y<!>
        }
    }

    interface I {
        fun test401(): KFunction0<Unit>
    }

    open inner class Inner : A(), I {
        private val y: String? = null

        fun test3(): String? = x
        fun test4(): String? = this.<!INVISIBLE_MEMBER!>x<!>

        inner class InnerInInner : Inner() {
            fun test40(): String? = x
            fun test41(): String? = y
        }

        private fun test400() {
        }

        override fun test401(): KFunction0<Unit> {
            return this::test400
        }
    }

    fun test5() {
        object : A() {
            fun local() {
                x
                this.<!INVISIBLE_MEMBER!>x<!>
            }

            inner class NestedInAnonymous() {
                fun test50(): String? = x
            }
        }
    }
}

fun A.extensionFun(): String? = this.<!INVISIBLE_MEMBER!>x<!>

abstract class B<T: B<T>> {
    protected abstract val thisBuilder: T
    private val x: String? = null

    fun test6(obj: Any?) = thisBuilder.apply {
        obj?.let { this.x }
    }
}