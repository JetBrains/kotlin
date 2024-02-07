// MODULE: lib

// FILE: some/graphics/Color.kt

package some.graphics

@JvmInline
value class Color(val value: Int) {
    companion object {
        val Blue = Color(42)
    }
}

// FILE: some/unit/ColorProvider.kt

package some.unit

import some.graphics.Color

var changingColor: Color? = null

class ColorProvider {
    val color: Color
        get() = changingColor ?: Color.Blue

    @Deprecated(message = "For binary compatibility") //, level = DeprecationLevel.HIDDEN)
    @get:JvmName("getColor")
    val deprecatedColor: Color?
        get() = changingColor
}

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

import some.unit.ColorProvider

fun test() {
    val color = ColorProvider().col<caret>or
}