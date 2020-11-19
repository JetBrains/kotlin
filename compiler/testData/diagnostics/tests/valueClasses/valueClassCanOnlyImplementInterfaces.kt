// !LANGUAGE: +InlineClasses

package kotlin

annotation class JvmInline

abstract class AbstractBaseClass

open class OpenBaseClass

interface BaseInterface

@JvmInline
value class TestExtendsAbstractClass(val x: Int) : <!INLINE_CLASS_CANNOT_EXTEND_CLASSES!>AbstractBaseClass<!>()

@JvmInline
value class TestExtendsOpenClass(val x: Int) : <!INLINE_CLASS_CANNOT_EXTEND_CLASSES!>OpenBaseClass<!>()

@JvmInline
value class TestImplementsInterface(val x: Int) : BaseInterface