// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82462
// LANGUAGE: +CompanionBlocks +CompanionExtensions

package test

import test.MyColor.EXT_COLOR_FROM_COMPANION

class MyColor(val name: String) {
    companion {
        <!COMPANION_BLOCK_MEMBER_EXTENSION!><!WRONG_MODIFIER_TARGET("companion; member property without backing field or delegate")!>companion<!> val MyColor.EXT_COLOR_FROM_COMPANION: MyColor<!> get() = MyColor("ext_color_from_companion")
    }
}

// Since the declaration-site is red, it's not important if we are able to resolve into it
val myColorCompanion: MyColor = <!UNRESOLVED_REFERENCE!>EXT_COLOR_FROM_COMPANION<!>

/* GENERATED_FIR_TAGS: classDeclaration, getter, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver,
stringLiteral */
