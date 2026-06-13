// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

class Tuple(val first: String, val second: Int)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class AnnExp

@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class Ann

fun declaration(x: Tuple) {
    if (true) { <!WRONG_ANNOTATION_TARGET!>@AnnExp<!> val ( first, second,) = x }
    if (true) { val (@Ann first) = x }
    if (true) { val (@Ann second) = x }
    if (true) { val (@Ann first, second) = x }
    if (true) { val (@Ann first, @Ann second) = x }
    if (true) { val (@Ann aa = first, second) = x }
    if (true) { val (@Ann aa: String = first) = x }
}

fun loop(xs: List<Tuple>) {
    <!ITERATOR_MISSING!>for (<!WRONG_ANNOTATION_TARGET!>@AnnExp (<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>first<!>, <!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>second<!>,)<!> <!WRONG_MODIFIER_TARGET!>in<!> xs<!SYNTAX!><!>) {}<!>
    for ((@Ann first) in xs) {}
    for ((@Ann second) in xs) {}
    for ((@Ann first, @Ann second) in xs) {}
    for ((@Ann aa = first, second) in xs) {}
    for ((@Ann aa: String = first) in xs) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { @AnnExp ( <!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>first<!>, <!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>second<!>)<!SYNTAX!><!> <!SYNTAX!>-><!> }
    foo { (@Ann first) -> }
    foo { (@Ann second) -> }
    foo { (@Ann first, @Ann second) -> }
    foo { (@Ann aa = first, second) -> }
    foo { (@Ann aa: String = first) -> }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, classDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
