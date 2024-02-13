// JSPECIFY_STATE: warn

// FILE: Foo.java
public interface Foo {}
// FILE: BaseClass.java

import org.jspecify.annotations.*;

@NullMarked
public class BaseClass {
    public Foo everythingNotNullable(Foo x) { return null; }

    public @Nullable Foo everythingNullable(@Nullable Foo x) { return null; }

    public @NullnessUnspecified Foo everythingUnknown(@NullnessUnspecified Foo x) { return null; }

    public @Nullable Foo mixed(Foo x) { return null; }

    public Foo explicitlyNullnessUnspecified(@NullnessUnspecified Foo x) { return null; }

    public void withVararg(Object... p) { }

    public static Foo foo() { return null; }
}


// FILE: main.kt

private val FOO = object : Foo {}

open class IntermediateClass : BaseClass() {
    open fun intermediateNotNull() = BaseClass.foo()
}

class Correct : IntermediateClass() {
    override fun everythingNotNullable(x: Foo): Foo {
        return FOO
    }

    override fun everythingNullable(x: Foo?): Foo? {
        return null
    }

    override fun everythingUnknown(x: Foo?): Foo? {
        return null
    }

    override fun mixed(x: Foo): Foo? {
        return null
    }

    override fun explicitlyNullnessUnspecified(x: Foo): Foo {
        return FOO
    }

    override fun intermediateNotNull(): Foo {
        return FOO
    }

    override fun withVararg(vararg p: Any) {}
}

class WrongReturnTypes : IntermediateClass() {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun everythingNotNullable(x: Foo): Foo? {
        return null
    }

    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun explicitlyNullnessUnspecified(x: Foo): Foo? {
        return null
    }

    override fun intermediateNotNull(): Foo? {
        return null
    }
}

class WrongParameter : IntermediateClass() {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun everythingNotNullable(x: Foo?): Foo {
        return FOO
    }

    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun everythingNullable(x: Foo): Foo? {
        return null
    }

    override fun everythingUnknown(x: Foo): Foo? {
        return null
    }

    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun mixed(x: Foo?): Foo? {
        return null
    }

    override fun explicitlyNullnessUnspecified(x: Foo?): Foo {
        return FOO
    }
}
