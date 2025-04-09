// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// WITH_STDLIB
// LANGUAGE: +MultiPlatformProjects
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// ISSUE: KT-76448

// MODULE: common
// FILE: common.kt
package kotlin

import kotlin.toString

public expect open class Throwable {
    public open val message: String?
    public open val cause: Throwable?

    public constructor()
    public constructor(message: String?)
    public constructor(cause: Throwable?)
    public constructor(message: String?, cause: Throwable?)
}
