// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82462

package test

import test.ColorExt.EXT_COLOR
import test.MyColor.Companion.EXT_COLOR_FROM_COMPANION

class MyColor(val name: String) {
    companion object {
        val MyColor.Companion.EXT_COLOR_FROM_COMPANION: MyColor get() = MyColor("ext_color_from_companion")
    }
}

object ColorExt {
    val MyColor.Companion.EXT_COLOR: MyColor get() = MyColor("ext_color")

    val myColor: MyColor = EXT_COLOR
}

val myColor: MyColor = <!UNRESOLVED_REFERENCE!>EXT_COLOR<!>
val myColorCompanion: MyColor = <!UNRESOLVED_REFERENCE!>EXT_COLOR_FROM_COMPANION<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, getter, objectDeclaration, primaryConstructor,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
