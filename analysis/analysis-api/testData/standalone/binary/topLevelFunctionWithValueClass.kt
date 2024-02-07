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

interface Context

interface ColorProvider {
    fun getColor(context: Context): Color
}

fun ColorProvider(color: Color): ColorProvider {
    return FixedColorProvider(color)
}

fun ColorProvider(resId: Int): ColorProvider {
    return ResourceColorProvider(resId)
}

data class FixedColorProvider(val color: Color) : ColorProvider {
    override fun getColor(context: Context) = color
}

data class ResourceColorProvider(val resId: Int) : ColorProvider {
    override fun getColor(context: Context): Color = Color(resId)
}

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

import some.graphics.Color
import some.unit.ColorProvider

fun test() {
    val color = Color<caret>Provider(color = Color.Blue)
}
