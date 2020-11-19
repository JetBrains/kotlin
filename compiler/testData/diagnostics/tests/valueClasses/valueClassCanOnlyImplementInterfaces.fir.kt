// !LANGUAGE: +InlineClasses

package kotlin

annotation class JvmInline

abstract class AbstractBaseClass

open class OpenBaseClass

interface BaseInterface

@JvmInline
value class TestExtendsAbstractClass(val x: Int) : AbstractBaseClass()

@JvmInline
value class TestExtendsOpenClass(val x: Int) : OpenBaseClass()

@JvmInline
value class TestImplementsInterface(val x: Int) : BaseInterface