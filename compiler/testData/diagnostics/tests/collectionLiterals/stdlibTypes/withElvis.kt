// LANGUAGE: +CollectionLiterals, +UnnamedLocalVariables
// RUN_PIPELINE_TILL: FRONTEND


fun <T> id(t: T): T = t
fun <T> nid(t: T): T? = t

class MyWithoutOf

class MyWithOf {
    companion object {
        operator fun of(vararg elem: Int) = MyWithOf()
    }
}

fun <T> nullableSet(): Set<T>? = null

fun test(
    a: MutableSet<Int>?,
    sa: MutableSet<*>?,
    nna: MutableSet<Int>,
    snna: MutableSet<*>,

    b: Collection<Int>?,
    sb: Collection<*>?,
    nnb: Collection<Int>,

    c: MyWithoutOf?,

    d: MyWithOf?,
) {
    val _ = a ?: []
    val _ = a ?: ["!"]
    val _ = sa ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _ = sa ?: [1, 2, 3]
    val _ = nna <!USELESS_ELVIS!>?: []<!>
    val _ = snna <!USELESS_ELVIS!>?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!><!>
    val _ = snna <!USELESS_ELVIS!>?: [1, 2, 3]<!>

    val _ = b ?: []
    val _ = b ?: ["!"]
    val _ = sb ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _ = sb ?: [1, 2, 3]
    val _ = nnb <!USELESS_ELVIS!>?: []<!>

    val _ = c ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _ = c ?: [1, 2, 3]

    val _ = d ?: []
    val _ = d ?: [1, 2, 3]
    val _ = d ?: [<!ARGUMENT_TYPE_MISMATCH!>"!"<!>]

    val _: Set<Int> = a ?: []
    val _: Collection<Int> = a ?: []
    val _: MutableCollection<Int> = a ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _: MutableCollection<*> = a ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>

    val _: Set<Int> <!INITIALIZER_TYPE_MISMATCH!>=<!> c ?: []
    val _: Collection<Int> <!INITIALIZER_TYPE_MISMATCH!>=<!> c ?: []
    val _: Any = c ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>

    /* ==== */

    val _ = a ?: id([])
    val _ = a ?: id(["!"])
    val _ = sa ?: id(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    val _ = sa ?: id(["!"])
    val _ = sa ?: nid(["!"])
    val _ = sa ?: nid(["!"]) ?: nid([1, 2, 3]) ?: nid([MyWithoutOf()]) ?: []

    val _ = nid(sa) ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _ = nid(sa) ?: [1, 2, 3]
    val _: Set<Int>? = nid(a) ?: nid([])
    val _: Collection<Int>? = nid(a) ?: nid([])
    val _: MutableCollection<Int>? = nid(a) ?: nid(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    val _: MutableCollection<*>? = nid(sa) ?: nid(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)

    val _ = <!CANNOT_INFER_PARAMETER_TYPE!>nullableSet<!>() ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _ = nullableSet() ?: [1, 2, 3]
    val _ = id(nullableSet()) ?: nid(["!"])

    val _ = <!CANNOT_INFER_PARAMETER_TYPE!>nid<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>) ?: <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    val _ = nid([1, 2, 3]) ?: id(["!"])
    val _ = id([]) <!USELESS_ELVIS!>?: nid(["!"])<!>
    val _ = nid([42]) ?: id([])
    val _ = id(nid([])) ?: [1, 2, 3]

    val _: Set<Int> = [] <!USELESS_ELVIS!>?: []<!>
    val _: Set<*> = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _: Collection<Int> = [] <!USELESS_ELVIS!>?: []<!>
    val _: MutableCollection<Int> = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val _: MutableCollection<Int> = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> ?: mutableSetOf()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, nullableType,
objectDeclaration, operator, propertyDeclaration, starProjection, typeParameter, unnamedLocalVariable, vararg */
