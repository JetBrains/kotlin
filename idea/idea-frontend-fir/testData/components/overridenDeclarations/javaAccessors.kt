// FILE: main.kt
class A : B() {
    override val x<caret>: Int get() = super.x
}

// FILE: B.java
public class B extends C {
    @Override
    public int getX() {
        return 0;
    }
}

// FILE: C.kt
abstract class C {
    abstract val x: Int
}


// RESULT
// ALL:
// B.x: Int
// C.x: Int

// DIRECT:
// B.x: Int
