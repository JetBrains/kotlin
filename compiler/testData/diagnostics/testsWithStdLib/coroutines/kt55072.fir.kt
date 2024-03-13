// ISSUE: KT-55072

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Player
{
    fun play() {
        ReentrantLock().withLock {
            launch {
                pumpEvents()
            }
            suspend fun launch2() {
                pumpEvents()
            }
            <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> {
                pumpEvents()
            }
            run {
                <!ILLEGAL_SUSPEND_FUNCTION_CALL!>pumpEvents<!>()
            }
        }
    }

    private suspend fun pumpEvents() {}

    private fun launch(block: suspend Any.() -> Unit) {}
}
