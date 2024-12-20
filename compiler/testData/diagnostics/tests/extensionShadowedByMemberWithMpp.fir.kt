// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// MODULE: common

expect class Test

expect val Test.number: Int

// MODULE: jvm()()(common)

actual class Test {
  val number = 10
}

actual val Test.number get() = this.number

// MODULE: js()()(common)

actual class Test

actual val Test.number get() = 20
