// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun stringF(): String = ""
fun nsf(): String? = "null"
fun unitF(): Unit = Unit

fun coll(m: MutableList<String>) {
    m.add("")
    <!RETURN_VALUE_NOT_USED!>m.isEmpty()<!>
}

fun nullable(m: MutableList<String>?) {
    m?.add("x")
    <!RETURN_VALUE_NOT_USED!>m?.isEmpty()<!>
}

fun exlusionPropagation(cond: Boolean, m: MutableList<String>) {
    if (cond) m.add("x") else throw IllegalStateException()
    if (cond) <!RETURN_VALUE_NOT_USED!>stringF()<!> else throw IllegalStateException()
}

@IgnorableReturnValue
fun discardable(): String = ""

fun unused(cond: Boolean) {
    <!RETURN_VALUE_NOT_USED!>stringF()<!>
    discardable()
    if (cond) discardable() else <!RETURN_VALUE_NOT_USED!>stringF()<!>
    if (cond) discardable() else unitF()
}

//fun underscore() {
//    val _ = stringF()
//}
