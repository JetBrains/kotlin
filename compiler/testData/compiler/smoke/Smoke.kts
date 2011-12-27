import kotlin.modules.*

val modules = module("smoke") {
    source files "Smoke.kt"
    jar name System.getProperty("java.io.tmpdir") + "/smoke.jar"
}
