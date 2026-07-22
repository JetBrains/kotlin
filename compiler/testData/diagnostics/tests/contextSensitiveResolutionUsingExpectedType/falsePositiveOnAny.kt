// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82461

open class MyColor(val name: String) {
  companion object {
    val COMPANION_COLOR = MyColor("companion_color")
    // Two receivers, shouldn't be resolved
    val Any.ANY_COLOR: MyColor get() = MyColor("companion_any_color")
  }
}

class DerivedColor : MyColor("derived")

// Does not extend MyColor or companion, shouldn't be resolved
val Any.ANY_COLOR: MyColor get() = MyColor("any_color")

val DerivedColor.DERIVED_COLOR: MyColor get() = MyColor("derived_color")

val myColor: MyColor = <!UNRESOLVED_REFERENCE!>ANY_COLOR<!>
val myResolvableColor: MyColor = COMPANION_COLOR
val myDerivedColor: MyColor = <!UNRESOLVED_REFERENCE!>DERIVED_COLOR<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, getter, objectDeclaration, primaryConstructor,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
