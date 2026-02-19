// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-56665

// CAPS is a typealias, Usual is a class
// --> is subtyping/aliasing edge, **> is type arguments edge
// Erroneous loop should include either typealiases only or subtyping/aliasing edges only

// Gau <** BAR --> Foo
//  |-------^
private typealias Bar = Foo<Gau>
internal class Gau : Bar

// Gau2 --> BAR2 --> Foo
//  ^*********|
internal class Gau2 : Bar2
private typealias Bar2 = Foo<Gau2>

// **> Cau --> Foo
// *****|
internal class Cau : Foo<Cau>
interface Foo<T>

// S1 <** ALIAS --> Foo
//  |-------^
class S : Foo<Alias>
typealias Alias = S

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nullableType, typeAliasDeclaration, typeParameter */
