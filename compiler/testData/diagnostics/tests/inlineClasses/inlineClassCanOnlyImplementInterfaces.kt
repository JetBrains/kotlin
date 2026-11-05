// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED -INLINE_CLASS_DEPRECATED_ERROR

abstract class AbstractBaseClass

open class OpenBaseClass

interface BaseInterface

inline class TestExtendsAbstractClass(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>AbstractBaseClass<!>()

inline class TestExtendsOpenClass(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>OpenBaseClass<!>()

inline class TestImplementsInterface(val x: Int) : BaseInterface

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, primaryConstructor, propertyDeclaration */
