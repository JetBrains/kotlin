// ISSUE: KT-90782

annotation class Ann

fun inline(s: String): Nothing = throw Exception(s)

fun value(f: () -> Unit) {}

fun private(s: String, f: () -> Unit = {}) {}

fun context(s: String) {}

fun localClass(): Int {
    class C
    error("boom")
}

fun localGenericClass(): Int {
    class C<T>
    inline("boom")
}

fun localAbstractClass() {
    abstract class C
    value {}
}

fun localDataClassWithParameters() {
    data class C(val x: Int)
    private("boom")
}

fun localClassWithSupertype() {
    open class Base
    class C : Base()
    private("boom") {}
}

fun localClassWithBody() {
    class C {}
    private("boom")
}

fun localClassFollowedByCallNamedContext() {
    class C
    context("boom")
}

fun localClassFollowedByCallWithArgumentsAndTrailingLambda() {
    class C
    private("boom") {}
}

fun localClassFollowedByAnnotatedCall() {
    class C
    @Suppress("UNUSED") private("boom")
}

fun localClassInLambda() {
    run {
        class C
        inline("boom")
    }
}

// Primary constructors with modifiers on the next line still belong to the class
fun localClassWithConstructorOnNextLine() {
    class C1
    private constructor()

    class C2
    @Ann constructor()

    class C3<T>
    internal constructor(x: T)

    class C4
    constructor()

    class C5
    @Ann
    private
    constructor(x: Int)
}

class TopLevel1
private constructor()

class TopLevel2
@Ann internal constructor(x: Int)
