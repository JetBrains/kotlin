trait <lineMarker descr="*"></lineMarker>SkipSupport {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportImpl<br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportWithDefaults</body></html>"></lineMarker>skip(why: String)
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportWithDefaults</body></html>"></lineMarker>skip()
}

public trait <lineMarker descr="*"></lineMarker>SkipSupportWithDefaults : SkipSupport {
    // TODO: should be "Is overriden in SkipSupportImpl"
    override fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportImpl</body></html>"><lineMarker descr="Implements function in 'SkipSupport'"></lineMarker></lineMarker>skip(why: String) {}

    override fun <lineMarker descr="Implements function in 'SkipSupport'"></lineMarker>skip() {
        skip("not given")
    }
}

open class SkipSupportImpl: SkipSupportWithDefaults {
    override fun <lineMarker descr="Overrides function in 'SkipSupportWithDefaults'"></lineMarker>skip(why: String) = throw RuntimeException(why)
}

// KT-4428 Incorrect override icon shown for overloaded methods