// FILE: Main.kt
const val name = <!EVALUATED: `SOME_PROPERTY`!>ClassWithProperty::SOME_PROPERTY.name<!>

// FILE: ClassWithProperty.java
public class ClassWithProperty {
    public boolean SOME_PROPERTY = false;
}
