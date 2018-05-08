// !LANGUAGE: +InlineClasses

inline class ConstructorWithDefaultVisibility(val x: Int)
inline class PublicConstructor public constructor(val x: Int)
inline class InternalConstructor <!NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS!>internal<!> constructor(val x: Int)
inline class ProtectedConstructor <!NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS!>protected<!> constructor(val x: Int)
inline class PrivateConstructor <!NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS!>private<!> constructor(val x: Int)
