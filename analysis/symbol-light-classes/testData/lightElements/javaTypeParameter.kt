// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)
// FILE: MyClass.java
public class MyClass {
    public <T<caret>T> void foo(TT xx) {}
}
