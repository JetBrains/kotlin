// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocks -CompanionExtensions
package test

import test.MyColor.<!UNRESOLVED_IMPORT!>Companion<!>.EXT_COLOR_FROM_COMPANION

class MyColor(val name: String) {
    <!UNSUPPORTED_FEATURE!>companion<!> {
        <!COMPANION_BLOCK_MEMBER_EXTENSION!>val MyColor.<!UNRESOLVED_REFERENCE!>Companion<!>.EXT_COLOR_FROM_COMPANION: MyColor<!> get() = MyColor("ext_color_from_companion")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver,
stringLiteral */
