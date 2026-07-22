// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocks -CompanionExtensions
package test

import test.MyColor.Companion.EXT_COLOR_FROM_COMPANION

class MyColor(val name: String) {
    companion {
        val MyColor.Companion.EXT_COLOR_FROM_COMPANION: MyColor get() = MyColor("ext_color_from_companion")
    }
}
