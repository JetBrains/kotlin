// FIR_IDENTICAL

interface Units<UValue : Any>

class SimpleDoubleUnits : Units<Double>

fun <BLeft : Any> build(builderCode: RendererBuilder<BLeft>.() -> Unit) {}

class RendererBuilder<RBLeft : Any> {
    var leftScaleCurves: CurveSet<RBLeft>? = null

    fun addDecorations(render: suspend RenderContext<RBLeft>.() -> Unit) {}
}

interface RenderContext<RCLeft : Any> {
    val leftScaleValueToY: ((RCLeft) -> Double)?
}

class State {
    suspend fun render() {
        build {
            leftScaleCurves = CurveSet(SimpleDoubleUnits())
            addDecorations {
                leftScaleValueToY!!(0.67)
            }
        }
    }
}

class CurveSet<CY : Any>(units: Units<CY>)
