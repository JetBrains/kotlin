actual sealed class <lineMarker>Sealed</lineMarker> {

    actual object <lineMarker>Sealed1</lineMarker> : Sealed()

    actual class <lineMarker>Sealed2</lineMarker> : Sealed() {
        actual val <lineMarker>x</lineMarker> = 42
        actual fun <lineMarker>foo</lineMarker>() = ""
    }
}


