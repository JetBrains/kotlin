// !LANGUAGE: +InlineClasses

abstract class AbstractBaseClass

open class OpenBaseClass

interface BaseInterface

inline class TestExtendsAbstractClass(val x: Int) : AbstractBaseClass()

inline class TestExtendsOpenClass(val x: Int) : OpenBaseClass()

inline class TestImplementsInterface(val x: Int) : BaseInterface