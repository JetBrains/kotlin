// DIAGNOSTICS: -UNUSED_PARAMETER, -NOTHING_TO_INLINE
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-70133

import kotlin.contracts.*

inline fun invokeInline(x: () -> Int, y: String) {
    contract { callsInPlace(x, InvocationKind.UNKNOWN) }
    x()
}

fun invokeInPlace(x: () -> Int, y: String) {
    contract { callsInPlace(x, InvocationKind.UNKNOWN) }
    x()
}

fun invokeLater(x: () -> Int, y: String) {
    x()
}

inline fun invokeInlineWithNoinline(noinline x: () -> Int, y: String) {
    x()
}

inline fun invokeInlineWithCrossinline(crossinline x: () -> Int, y: String) {
    x()
}

inline fun <T> invokeInlineWithT(x: () -> T, y: T) {
    x()
}

fun immutableInitAfterCaptureInline() {
    val x: String
    invokeInline(
        x = { x.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun immutableInitAfterCaptureInPlace() {
    val x: String
    invokeInPlace(
        x = { x.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun immutableInitAfterCaptureLater() {
    val x: String
    invokeLater(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun immutableInitAfterCaptureInlineWithNoinline() {
    val x: String
    invokeInlineWithNoinline(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun immutableInitAfterCaptureInlineWithCrossinline() {
    val x: String
    invokeInlineWithCrossinline(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun immutableInitAfterCaptureInlineWithT() {
    val x: String
    invokeInlineWithT(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun immutableInitBeforeCaptureInline() {
    val x: String
    invokeInline(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun immutableInitBeforeCaptureInPlace() {
    val x: String
    invokeInPlace(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun immutableInitBeforeCaptureLater() {
    val x: String
    invokeLater(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun immutableInitBeforeCaptureInlineWithNoinline() {
    val x: String
    invokeInlineWithNoinline(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun immutableInitBeforeCaptureInlineWithCrossinline() {
    val x: String
    invokeInlineWithCrossinline(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun immutableInitBeforeCaptureInlineWithT() {
    val x: String
    invokeInlineWithT(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun mutableInitAfterCaptureInline() {
    var x: String
    invokeInline(
        x = { x.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun mutableInitAfterCaptureInPlace() {
    var x: String
    invokeInPlace(
        x = { x.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun mutableInitAfterCaptureLater() {
    var x: String
    invokeLater(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun mutableInitAfterCaptureInlineWithNoinline() {
    var x: String
    invokeInlineWithNoinline(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun mutableInitAfterCaptureInlineWithCrossinline() {
    var x: String
    invokeInlineWithCrossinline(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun mutableInitAfterCaptureInlineWithT() {
    var x: String
    invokeInlineWithT(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun mutableInitBeforeCaptureInline() {
    var x: String
    invokeInline(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun mutableInitBeforeCaptureInPlace() {
    var x: String
    invokeInPlace(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun mutableInitBeforeCaptureLater() {
    var x: String
    invokeLater(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun mutableInitBeforeCaptureInlineWithNoinline() {
    var x: String
    invokeInlineWithNoinline(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun mutableInitBeforeCaptureInlineWithCrossinline() {
    var x: String
    invokeInlineWithCrossinline(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun mutableInitBeforeCaptureInlineWithT() {
    var x: String
    invokeInlineWithT(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

class Inline {
    val immutableBefore: String
    val immutableAfter: String
    var mutableBefore: String
    var mutableAfter: String

    init {
        invokeInline(
            x = { immutableBefore.length },
            y = if (true) { immutableBefore = ""; "" } else { immutableBefore = ""; "" },
        )
        invokeInline(
            y = if (true) { immutableAfter = ""; "" } else { immutableAfter = ""; "" },
            x = { immutableAfter.length },
        )
        invokeInline(
            x = { mutableBefore.length },
            y = if (true) { mutableBefore = ""; "" } else { mutableBefore = ""; "" },
        )
        invokeInline(
            y = if (true) { mutableAfter = ""; "" } else { mutableAfter = ""; "" },
            x = { mutableAfter.length },
        )
    }
}

class InPlace {
    val immutableBefore: String
    val immutableAfter: String
    var mutableBefore: String
    var mutableAfter: String

    init {
        invokeInPlace(
            x = { immutableBefore.length },
            y = if (true) { immutableBefore = ""; "" } else { immutableBefore = ""; "" },
        )
        invokeInPlace(
            y = if (true) { immutableAfter = ""; "" } else { immutableAfter = ""; "" },
            x = { immutableAfter.length },
        )
        invokeInPlace(
            x = { mutableBefore.length },
            y = if (true) { mutableBefore = ""; "" } else { mutableBefore = ""; "" },
        )
        invokeInPlace(
            y = if (true) { mutableAfter = ""; "" } else { mutableAfter = ""; "" },
            x = { mutableAfter.length },
        )
    }
}

class Later {
    val immutableBefore: String
    val immutableAfter: String
    var mutableBefore: String
    var mutableAfter: String

    init {
        invokeLater(
            x = { <!UNINITIALIZED_VARIABLE!>immutableBefore<!>.length },
            y = if (true) { immutableBefore = ""; "" } else { immutableBefore = ""; "" },
        )
        invokeLater(
            y = if (true) { immutableAfter = ""; "" } else { immutableAfter = ""; "" },
            x = { immutableAfter.length },
        )
        invokeLater(
            x = { <!UNINITIALIZED_VARIABLE!>mutableBefore<!>.length },
            y = if (true) { mutableBefore = ""; "" } else { mutableBefore = ""; "" },
        )
        invokeLater(
            y = if (true) { mutableAfter = ""; "" } else { mutableAfter = ""; "" },
            x = { mutableAfter.length },
        )
    }
}

class NoInline {
    val immutableBefore: String
    val immutableAfter: String
    var mutableBefore: String
    var mutableAfter: String

    init {
        invokeInlineWithNoinline(
            x = { <!UNINITIALIZED_VARIABLE!>immutableBefore<!>.length },
            y = if (true) { immutableBefore = ""; "" } else { immutableBefore = ""; "" },
        )
        invokeInlineWithNoinline(
            y = if (true) { immutableAfter = ""; "" } else { immutableAfter = ""; "" },
            x = { immutableAfter.length },
        )
        invokeInlineWithNoinline(
            x = { <!UNINITIALIZED_VARIABLE!>mutableBefore<!>.length },
            y = if (true) { mutableBefore = ""; "" } else { mutableBefore = ""; "" },
        )
        invokeInlineWithNoinline(
            y = if (true) { mutableAfter = ""; "" } else { mutableAfter = ""; "" },
            x = { mutableAfter.length },
        )
    }
}

class CrossInline {
    val immutableBefore: String
    val immutableAfter: String
    var mutableBefore: String
    var mutableAfter: String

    init {
        invokeInlineWithCrossinline(
            x = { <!UNINITIALIZED_VARIABLE!>immutableBefore<!>.length },
            y = if (true) { immutableBefore = ""; "" } else { immutableBefore = ""; "" },
        )
        invokeInlineWithCrossinline(
            y = if (true) { immutableAfter = ""; "" } else { immutableAfter = ""; "" },
            x = { immutableAfter.length },
        )
        invokeInlineWithCrossinline(
            x = { <!UNINITIALIZED_VARIABLE!>mutableBefore<!>.length },
            y = if (true) { mutableBefore = ""; "" } else { mutableBefore = ""; "" },
        )
        invokeInlineWithCrossinline(
            y = if (true) { mutableAfter = ""; "" } else { mutableAfter = ""; "" },
            x = { mutableAfter.length },
        )
    }
}

class InlineWithT {
    val immutableBefore: String
    val immutableAfter: String
    var mutableBefore: String
    var mutableAfter: String

    init {
        invokeInlineWithT(
            x = { <!UNINITIALIZED_VARIABLE!>immutableBefore<!>.length },
            y = if (true) { immutableBefore = ""; "" } else { immutableBefore = ""; "" },
        )
        invokeInlineWithT(
            y = if (true) { immutableAfter = ""; "" } else { immutableAfter = ""; "" },
            x = { immutableAfter.length },
        )
        invokeInlineWithT(
            x = { <!UNINITIALIZED_VARIABLE!>mutableBefore<!>.length },
            y = if (true) { mutableBefore = ""; "" } else { mutableBefore = ""; "" },
        )
        invokeInlineWithT(
            y = if (true) { mutableAfter = ""; "" } else { mutableAfter = ""; "" },
            x = { mutableAfter.length },
        )
    }
}
