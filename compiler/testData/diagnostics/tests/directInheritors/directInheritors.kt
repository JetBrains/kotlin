// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +DirectClassInheritors

abstract class A
interface I<T>
class B : A(), I<Int>

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration */
