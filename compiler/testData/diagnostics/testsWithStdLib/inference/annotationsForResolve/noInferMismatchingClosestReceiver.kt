// RUN_PIPELINE_TILL: FRONTEND
// DONT_WARN_ON_ERROR_SUPPRESSION
// ISSUE: KT-77545

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> (@kotlin.internal.NoInfer T).function() {}

class KlassA
class KlassB

fun main() {
    with(KlassA()) {
        with(KlassB()) {
            function<KlassA>()
        }
    }
}

fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()
