// LANGUAGE: +StrictEquals
// function: /JavaCombined.equals(other)

// FILE: main.kt
abstract class ClassBound {
    override fun equals(@EqualityBound(ClassBound::class) other: Any?): Boolean = true
}

interface InterfaceBound {
    override fun equals(@EqualityBound(InterfaceBound::class) other: Any?): Boolean
}

// FILE: JavaCombined.java
public final class JavaCombined extends ClassBound implements InterfaceBound {
    @Override
    public boolean equals(Object other) {
        return false;
    }
}
