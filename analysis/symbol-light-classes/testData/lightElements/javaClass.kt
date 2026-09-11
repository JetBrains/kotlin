// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject(MyClass)
// FILE: MyClass.java
public class My<caret>Class {
    public void foo(int x) {}
}
