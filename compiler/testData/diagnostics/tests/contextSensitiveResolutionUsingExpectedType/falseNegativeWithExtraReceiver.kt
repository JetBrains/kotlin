// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82465

class MyColor(val name: String) {
    companion object {
        val COMPANION_COLOR = MyColor("companion_color")
    }
}

val MyColor.Companion.EXT_COLOR_TOP: MyColor get() = MyColor("ext_color")

fun String.test() {
    val myColorLocal: MyColor = EXT_COLOR_TOP
    val myColorCompanionLocal: MyColor = COMPANION_COLOR
}

class Another {
    val myColorMember: MyColor = EXT_COLOR_TOP
    val myColorCompanionMember: MyColor = COMPANION_COLOR

    fun usage() {
        val myColorMemberLocal: MyColor = EXT_COLOR_TOP
        val myColorCompanionMemberLocal: MyColor = COMPANION_COLOR
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, getter,
localProperty, objectDeclaration, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
