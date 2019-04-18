// !LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt
package foo

// explicitly: both duplicated declaration have 'actual' modifier
// implicitly: only one duplicated declaration has 'actual' modifier, but both are matched

expect class ExplicitlyDuplicatedByMiddleAndJs
expect class ImplicitlyDuplicatedByMiddleAndJs

expect class ExplicitlyDuplicatedByMiddleAndJvm
expect class ImplicitlyDuplicatedByMiddleAndJvm


// MODULE: jvmAndJs(common)
// FILE: jvmAndJs.kt
package foo

actual class ExplicitlyDuplicatedByMiddleAndJs
class ImplicitlyDuplicatedByMiddleAndJs

actual class ExplicitlyDuplicatedByMiddleAndJvm
actual class ImplicitlyDuplicatedByMiddleAndJvm

// MODULE: jvm(jvmAndJs)
// FILE: jvm.kt
package foo

actual class ExplicitlyDuplicatedByMiddleAndJvm
class ImplicitlyDuplicatedByMiddleAndJvm


// MODULE: js(jvmAndJs)
// FILE: js.kt
package foo

actual class ExplicitlyDuplicatedByMiddleAndJs
actual class ImplicitlyDuplicatedByMiddleAndJs
