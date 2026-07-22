// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82462

package test

import test.ColorExt.EXT_COLOR

class MyColor(val name: String) {
    companion object
}

object ColorExt {
    val MyColor.Companion.EXT_COLOR: MyColor get() = MyColor("ext_color")
}

val myColor: MyColor = EXT_COLOR

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, getter, objectDeclaration, primaryConstructor,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
