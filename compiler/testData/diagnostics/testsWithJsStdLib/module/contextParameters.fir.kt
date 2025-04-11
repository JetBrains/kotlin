// LANGUAGE: +ContextParameters
@file:JsModule("lib")

external class Scope1
external class Scope2

context(scope1: Scope1, scope2: Scope2)
external fun foo()

context(scope2: Scope2, scope1: Scope1)
external fun foo()
