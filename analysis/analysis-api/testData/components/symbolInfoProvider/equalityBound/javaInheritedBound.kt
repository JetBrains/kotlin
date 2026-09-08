// LANGUAGE: +StrictEquals
// function: /JavaDerived.equals(other)

// FILE: main.kt
open class Base {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = true
}

// FILE: JavaDerived.java
public class JavaDerived extends Base {
    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }
}
