// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82461

class MyColor(val name: String) {
  companion object {
    val COMPANION_COLOR = MyColor("companion_color")
  }
}

val Any.ANY_COLOR: MyColor get() = MyColor("any_color")

val myColor: MyColor = ANY_COLOR

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, getter, objectDeclaration, primaryConstructor,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
