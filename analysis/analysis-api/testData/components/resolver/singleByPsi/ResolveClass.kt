// The frontend resolves to the Test class.
//
// The analysis API adjusts the resolution to be the constructor as that is what the IDE expects.
//
// In this particular case, there is no PSI element for the constructor. Therefore PSI
// resolution `KtReference.resolve()` returns the KtClass for Test whereas symbol resolution
// `KtReference.resolveToSymbol` returns the KtConstructorSymbol for the Test constructor.
//
// This test is testing symbol resolution, so the resolution result is
// the constructor in Test.

open class Test

class SomeTest : <caret>Test()
