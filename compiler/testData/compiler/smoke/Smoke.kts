import kotlin.modules.*

val modules = module("smoke") {
    source files "src/Smoke.kt"
    testSource files "test/SmokeTest.kt"
    jar name System.getProperty("java.io.tmpdir") + "/smoke.jar"
}
