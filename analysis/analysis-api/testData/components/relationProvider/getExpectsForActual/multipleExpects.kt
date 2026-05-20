// LANGUAGE: +MultiPlatformProjects

// MODULE: common1
// FILE: Common1.kt

package sample

expect fun foo()

// MODULE: common2
// FILE: Common2.kt

package sample

expect fun foo()

// MODULE: jvm()()(common1, common2)
// FILE: Jvm.kt

package sample

<expr>actual fun foo() {}</expr>
