// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

enum class MyEnum { X, Y }

fun addEnum(x: MutableList<in MyEnum>) {
    x.add(X)
    x.add(MyEnum.Y)
}

fun addSet(x: MutableList<in Set<String>>) {
    x.add([])
    x.add(["a"])
}

fun <T : MyEnum> addBound(x: MutableList<in T>) {
    x.add(<!ARGUMENT_TYPE_MISMATCH!>X<!>)
}

fun addOut(x: MutableList<out MyEnum>, y: List<<!REDUNDANT_PROJECTION!>out<!> Set<String>>) {
    x.add(<!UNRESOLVED_REFERENCE!>X<!>)
    y.contains([])
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, inProjection, outProjection, stringLiteral,
typeConstraint, typeParameter */
