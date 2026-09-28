// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232
// FIR_DUMP
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

enum class MyEnum { X, Y }
enum class OtherEnum { Z }

class Box(val elements: List<MyEnum>) {
    companion object {
        operator fun of(vararg elements: MyEnum): Box = TODO()
    }
}

class OtherBox(val elements: List<OtherEnum>) {
    companion object {
        operator fun of(vararg elements: OtherEnum): OtherBox = TODO()
    }
}

fun takeBox(b: Box) {}
fun takeBox(o: OtherBox) {}

fun takeList(l: List<MyEnum>) {}
fun takeList(s: Set<OtherEnum>) {}

fun main() {
    val b: Box = [X, Y]
    val o: OtherBox = [Z]
    val l: List<MyEnum> = [X, Y]

    takeBox([X])
    takeBox([Z])
    <!NONE_APPLICABLE!>takeBox<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!UNRESOLVED_REFERENCE!>X<!>, <!UNRESOLVED_REFERENCE!>Q<!>]<!>)

    // CSR parts have type variable expected types, so they're not eagerly resolved
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>takeList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!UNRESOLVED_REFERENCE!>Y<!>]<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>takeList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!UNRESOLVED_REFERENCE!>Z<!>]<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>takeList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!UNRESOLVED_REFERENCE!>Y<!>, <!UNRESOLVED_REFERENCE!>Q<!>]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration, localProperty,
objectDeclaration, operator, primaryConstructor, propertyDeclaration, vararg */
