// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
<!WRONG_MODIFIER_TARGET!>error<!> class Foo

typealias TA = RichError

fun <T, V : Value, E : RichError, E2 : TA, E3 : E, E4 : V | Foo, E5: E | Foo> test(
    a: T | Foo,
    b: V | Foo,
    c: E | Foo,
    d: E2 | Foo,
    e: <!OTHER_ERROR!>Foo | T<!>,
    f: <!OTHER_ERROR!>Foo | V<!>,
    g: Foo | E,
    h: Foo | E2,
    i: String | E3,
    j: E4 | Foo,
    k: E5 | Foo,
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

fun <G : CharSequence, T : List<F | Foo>, F : G | Foo> boundsWithUnionAndDependency() {}
val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>G : CharSequence<!>, T : List<F | Foo>, <!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>F : G | Foo<!>> T.boundsWithUnionAndDependency get() = 1
interface I<G : CharSequence, T : List<F | Foo>, F : G | Foo>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeConstraint, typeParameter */
