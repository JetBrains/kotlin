interface <lineMarker descr="*">SkipSupport</lineMarker> {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportImpl<br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportWithDefaults</body></html>">skip</lineMarker>(why: String)
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportWithDefaults</body></html>">skip</lineMarker>()
}

public interface <lineMarker descr="*">SkipSupportWithDefaults</lineMarker> : SkipSupport {
    // TODO: should be "Is overriden in SkipSupportImpl"
    override fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportImpl</body></html>"><lineMarker descr="Implements function in 'SkipSupport'">skip</lineMarker></lineMarker>(why: String) {}

    override fun <lineMarker descr="Implements function in 'SkipSupport'">skip</lineMarker>() {
        skip("not given")
    }
}

open class SkipSupportImpl: SkipSupportWithDefaults {
    override fun <lineMarker descr="Overrides function in 'SkipSupportWithDefaults'">skip</lineMarker>(why: String) = throw RuntimeException(why)
}

// KT-4428 Incorrect override icon shown for overloaded methods