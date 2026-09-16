// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
<!WRONG_MODIFIER_TARGET!>error<!> class Foo

typealias TA = RichError

fun <T, V : Value, E : RichError, E2 : TA> test(
    a: T | Foo,
    b: V | Foo,
    c: E | Foo,
    d: E2 | Foo,
    e: <!OTHER_ERROR!>Foo | T<!>,
    f: <!OTHER_ERROR!>Foo | V<!>,
    g: Foo | E,
    h: Foo | E2,
) {}

val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>E : RichError<!>> (E | Foo).prop get() = 1

class C<T, V : Value, E : RichError, E2 : TA> {
    fun test(
        a: T | Foo,
        b: V | Foo,
        c: E | Foo,
        d: E2 | Foo,
        e: <!OTHER_ERROR!>Foo | T<!>,
        f: <!OTHER_ERROR!>Foo | V<!>,
        g: Foo | E,
        h: Foo | E2,
    ) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeConstraint, typeParameter */
