// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameter(xx)
// FILE: MyClass.java
public class MyClass {
    public <TT> void foo(TT x<caret>x) {}
}
