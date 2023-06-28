// !LANGUAGE: +ProhibitProtectedCallFromInline
// !DIAGNOSTICS: -EXPOSED_PARAMETER_TYPE -NOTHING_TO_INLINE

// FIR_IDENTICAL

// FILE: JavaClass.java


public abstract class JavaClass {
    protected void bind() {}
}

// FILE: main.kt
open class A {
    protected fun test() {}

    protected val z: String = "1"

    public var zVar: String = "1"
        protected set(value) {}

    inline fun call() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>test<!>()
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>z<!>
        zVar
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>zVar<!> = "123"
    }

    internal inline fun callFromInternal() {
        test()
        zVar
        zVar = "123"
    }

    @PublishedApi
    internal inline fun callFromPublished() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>test<!>()
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>z<!>
        zVar
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>zVar<!> = "123"
    }

    protected inline fun callFromProtected() {
        test()
        zVar
        zVar = "123"
    }

}

class B : A() {
    inline fun testB() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>test<!>()
    }
}

class C : JavaClass() {
    inline fun call() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>bind<!>()
    }

    internal inline fun callFromInternal() {
        bind()
    }

    protected inline fun callFromProtected() {
        bind()
    }

    @PublishedApi
    internal inline fun callFromPublished() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>bind<!>()
    }
}


internal class AInternal {
    protected fun test() {}

    protected val z: String = "1"

    public var zVar: String = "1"
        protected set(value) {}


    inline fun call() {
        test()
    }

    @PublishedApi
    internal inline fun call2() {
        test()
    }
}

private class X {

    public class Z : A() {
        public inline fun effictivelyNonPublic() {
            test()
        }
    }

}
