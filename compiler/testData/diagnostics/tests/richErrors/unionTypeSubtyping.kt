// RUN_PIPELINE_TILL: FRONTEND

<!WRONG_MODIFIER_TARGET!>error<!> class Foo
<!WRONG_MODIFIER_TARGET!>error<!> class Bar

var a: CharSequence = ""
var b: CharSequence | Foo = ""
var c: CharSequence | Foo | Bar = ""
var d: Foo | Bar = Foo()

fun test() {
    a = a
    a = <!ASSIGNMENT_TYPE_MISMATCH!>b<!>
    a = <!ASSIGNMENT_TYPE_MISMATCH!>c<!>
    a = <!ASSIGNMENT_TYPE_MISMATCH!>d<!>
    a = ""

    b = a
    b = b
    b = <!ASSIGNMENT_TYPE_MISMATCH!>c<!>
    b = <!ASSIGNMENT_TYPE_MISMATCH!>d<!>
    b = ""

    c = a
    c = b
    c = c
    c = d
    c = ""

    d = <!ASSIGNMENT_TYPE_MISMATCH!>a<!>
    d = <!ASSIGNMENT_TYPE_MISMATCH!>b<!>
    d = <!ASSIGNMENT_TYPE_MISMATCH!>c<!>
    d = d
    d <!ASSIGNMENT_TYPE_MISMATCH!>=<!> ""
}

fun foo(l: List<CharSequence | Foo>) {
    foo(l <!UNCHECKED_CAST!>as List<CharSequence><!>)
    foo(l <!UNCHECKED_CAST!>as List<String><!>)
    foo(l <!UNCHECKED_CAST!>as List<Foo><!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>l <!UNCHECKED_CAST!>as List<Bar><!><!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
