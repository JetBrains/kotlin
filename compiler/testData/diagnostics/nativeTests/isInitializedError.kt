// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -NOTHING_TO_INLINE
// !LANGUAGE: +NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// FILE: stdlibInternal.kt

package kotlin.internal

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class AccessibleLateinitPropertyLiteral

// FILE: stdlib.kt
package kotlin

import kotlin.internal.AccessibleLateinitPropertyLiteral
import kotlin.reflect.KProperty0

inline val @receiver:AccessibleLateinitPropertyLiteral KProperty0<*>.isInitialized: Boolean
    get() = true


// FILE: test.kt

interface Base {
    var x: String
}

open class Foo : Base {
    override lateinit var x: String
    private lateinit var y: String

    var nonLateInit: Int = 1

    fun ok() {
        val b: Boolean = this::x.isInitialized

        val otherInstance = Foo()
        otherInstance::x.isInitialized

        (this::x).isInitialized
        (@Suppress("ALL") (this::x)).isInitialized

        object {
            fun local() {
                class Local {
                    val xx = this@Foo::x.isInitialized
                    val yy = this@Foo::y.isInitialized
                }
            }
        }
    }

    fun onLiteral() {
        val p = this::x
        p.<!LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL!>isInitialized<!>
    }

    fun onNonLateinit() {
        this::nonLateInit.<!LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT!>isInitialized<!>
    }

    inline fun inlineFun() {
        this::x.<!LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION!>isInitialized<!>

        object {
            val z = this@Foo::x.isInitialized
        }
    }

    inner class InnerSubclass : Foo() {
        fun innerOk() {
            // This is access to Foo.x declared lexically above
            this@Foo::x.isInitialized

            // This is access to InnerSubclass.x which is inherited from Foo.x
            this::x.isInitialized
        }
    }
}

fun onNonAccessible() {
    Foo()::x.<!LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY!>isInitialized<!>
}

fun onNonLateinit() {
    Foo()::nonLateInit.<!LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT!>isInitialized<!>
}

object Unrelated {
    fun onNonAccessible() {
        Foo()::x.<!LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY!>isInitialized<!>
    }
}

class FooImpl : Foo() {
    fun onNonAccessible() {
        this::x.<!LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY!>isInitialized<!>
    }
}

// FILE: other.kt

class OtherFooImpl : Foo() {
    fun onNonAccessible() {
        this::x.<!LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY!>isInitialized<!>
    }
}
