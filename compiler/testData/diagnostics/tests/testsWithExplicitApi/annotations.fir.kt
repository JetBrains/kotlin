// SKIP_TXT

annotation class A

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION
)
public annotation class B

annotation class C(val a: String)

/**
 * Foo1 KDoc
 */
@B
class Foo1() {}

public class Foo2() {
    /**
     * KDoc for methodWithAnnotations
     */
    @B
    fun methodWithAnnotations() {}

    /**
     * Property KDoc
     */
    @B
    var simple: Int = 10
}

public open class ClassWithOpen {
    /**
     * constructor KDoc
     */
    @B
    constructor() {}

    /**
     * KDoc for openAnnotatedMethod
     */
    @B
    open fun openAnnotatedMethod() {}
}