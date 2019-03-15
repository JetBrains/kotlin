// !LANGUAGE: +InlineClasses

abstract class AbstractBaseClass

open class OpenBaseClass

interface BaseInterface

inline class TestExtendsAbstractClass(val x: Int) : <!INLINE_CLASS_CANNOT_EXTEND_CLASSES!>AbstractBaseClass<!>()

inline class TestExtendsOpenClass(val x: Int) : <!INLINE_CLASS_CANNOT_EXTEND_CLASSES!>OpenBaseClass<!>()

inline class TestImplementsInterface(val x: Int) : BaseInterface