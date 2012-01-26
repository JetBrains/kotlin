import kotlin.modules.*

val homeDir = "../../../testlib/test"

fun project() {
    module("Testlib") {
        sources += homeDir
    }
}
