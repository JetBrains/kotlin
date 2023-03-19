// FIR_IDENTICAL

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

interface MkMutableSharedSettingsHolder {
    var cleanTarget: String?
}

data class MakefileSettingsFacade(
    val projectSettings: MkMutableSharedSettingsHolder
): MkMutableSharedSettingsHolder by projectSettings {
    var cleanTarget2: String? = ""
}

fun <T> consume(arg: T) {}

fun foo(arg: MakefileSettingsFacade) {
    consume<KProperty0<String?>>(arg::cleanTarget)
    consume<KMutableProperty0<String?>>(arg::cleanTarget)
}
