// FIR_IDENTICAL
// SKIP_TXT

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>annotation class A<!>

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION
)
public annotation class B

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>annotation class C<!>(val a: String)

/**
 * Foo1 KDoc
 */
@B
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>class Foo1<!>() {}

public class Foo2() {
    /**
     * KDoc for methodWithAnnotations
     */
    @B
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun methodWithAnnotations<!>() {}

    /**
     * Property KDoc
     */
    @B
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>var simple<!>: Int = 10
}

public open class ClassWithOpen {
    /**
     * constructor KDoc
     */
    @B
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>constructor<!>() {}

    /**
     * KDoc for openAnnotatedMethod
     */
    @B
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>open fun openAnnotatedMethod<!>() {}
}