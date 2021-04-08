// FILE: kt45853.kt
open class MyProblem() : ThrowableProblem() {
    override fun getCause(): Exceptional? = super.cause
}

// FILE: Exceptional.java
public interface Exceptional {
    Exceptional getCause();
}

// FILE: ThrowableProblem.java
public abstract class ThrowableProblem extends RuntimeException implements Exceptional  {
    @Override
    public ThrowableProblem getCause() {
        // cast is safe, since the only way to set this is our constructor
        return (ThrowableProblem) super.getCause();
    }
}
