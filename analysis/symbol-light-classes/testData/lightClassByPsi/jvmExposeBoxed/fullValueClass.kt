// LANGUAGE: +FullValueClasses
// LIBRARY_PLATFORMS: JVM

@file:OptIn(ExperimentalStdlibApi::class)

package exposed

value class FullValue @JvmExposeBoxed constructor(val x: Int = 0, val y: String = "") {
    @JvmExposeBoxed
    fun render(): String = "$x: $y"
}

@JvmExposeBoxed
class Usage(val value: FullValue) {
    @get:JvmExposeBoxed
    @set:JvmExposeBoxed
    var mutable: FullValue = value

    @JvmExposeBoxed
    fun consume(value: FullValue): FullValue = value
}
