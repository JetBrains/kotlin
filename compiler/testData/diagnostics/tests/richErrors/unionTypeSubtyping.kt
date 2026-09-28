// RUN_PIPELINE_TILL: FRONTEND

<!WRONG_MODIFIER_TARGET!>error<!> class Foo
<!WRONG_MODIFIER_TARGET!>error<!> class Bar

var a: CharSequence = ""
var b: CharSequence | Foo = ""
var c: CharSequence | Foo | Bar = ""
var d: CharSequence? | Foo | Bar = ""
var e: Foo | Bar = Foo()
var f: (Foo | Bar)? = null

fun test() {
    a = a
    a = <!ASSIGNMENT_TYPE_MISMATCH!>b<!>
    a = <!ASSIGNMENT_TYPE_MISMATCH!>c<!>
    a = <!ASSIGNMENT_TYPE_MISMATCH!>d<!>
    a = <!ASSIGNMENT_TYPE_MISMATCH!>e<!>
    a = <!ASSIGNMENT_TYPE_MISMATCH!>f<!>
    a = ""
    a = <!NULL_FOR_NONNULL_TYPE!>null<!>
    a <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as CharSequence?
    a <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as String?
    a <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as Foo?
    a <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as Bar?
    a = <!ASSIGNMENT_TYPE_MISMATCH!>Foo<!>()
    a = <!ASSIGNMENT_TYPE_MISMATCH!>Bar<!>()

    b = a
    b = b
    b = <!ASSIGNMENT_TYPE_MISMATCH!>c<!>
    b = <!ASSIGNMENT_TYPE_MISMATCH!>d<!>
    b = <!ASSIGNMENT_TYPE_MISMATCH!>e<!>
    b = <!ASSIGNMENT_TYPE_MISMATCH!>f<!>
    b = ""
    b = <!NULL_FOR_NONNULL_TYPE!>null<!>
    b <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as CharSequence?
    b <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as String?
    b <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as Foo?
    b <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as Bar?
    b = Foo()
    b = <!ASSIGNMENT_TYPE_MISMATCH!>Bar<!>()

    c = a
    c = b
    c = c
    c = <!ASSIGNMENT_TYPE_MISMATCH!>d<!>
    c = e
    c = <!ASSIGNMENT_TYPE_MISMATCH!>f<!>
    c = ""
    c = <!NULL_FOR_NONNULL_TYPE!>null<!>
    c <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as CharSequence?
    c <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as String?
    c <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as Foo?
    c <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as Bar?
    c = Foo()
    c = Bar()

    d = a
    d = b
    d = c
    d = d
    d = e
    d = f
    d = ""
    d = null
    d = null as CharSequence?
    d = null as String?
    d = null as Foo?
    d = null as Bar?
    d = Foo()
    d = Bar()

    e = <!ASSIGNMENT_TYPE_MISMATCH!>a<!>
    e = <!ASSIGNMENT_TYPE_MISMATCH!>b<!>
    e = <!ASSIGNMENT_TYPE_MISMATCH!>c<!>
    e = <!ASSIGNMENT_TYPE_MISMATCH!>d<!>
    e = e
    e = <!ASSIGNMENT_TYPE_MISMATCH!>f<!>
    e <!ASSIGNMENT_TYPE_MISMATCH!>=<!> ""
    e = <!NULL_FOR_NONNULL_TYPE!>null<!>
    e <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as CharSequence?
    e <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as String?
    e <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as Foo?
    e <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as Bar?
    e = Foo()
    e = Bar()

    f = <!ASSIGNMENT_TYPE_MISMATCH!>a<!>
    f = <!ASSIGNMENT_TYPE_MISMATCH!>b<!>
    f = <!ASSIGNMENT_TYPE_MISMATCH!>c<!>
    f = <!ASSIGNMENT_TYPE_MISMATCH!>d<!>
    f = e
    f = f
    f <!ASSIGNMENT_TYPE_MISMATCH!>=<!> ""
    f = null
    f <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as CharSequence?
    f <!ASSIGNMENT_TYPE_MISMATCH!>=<!> null as String?
    f = null as Foo?
    f = null as Bar?
    f = Foo()
    f = Bar()
}

fun foo(l: List<CharSequence | Foo>) {
    foo(l <!UNCHECKED_CAST!>as List<CharSequence><!>)
    foo(l <!UNCHECKED_CAST!>as List<String><!>)
    foo(l <!UNCHECKED_CAST!>as List<Foo><!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>l <!UNCHECKED_CAST!>as List<Bar><!><!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
