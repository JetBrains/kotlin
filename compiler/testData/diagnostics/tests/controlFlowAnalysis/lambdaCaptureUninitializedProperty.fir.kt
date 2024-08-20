// DIAGNOSTICS: -UNUSED_PARAMETER
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
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
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
        x = { x.length },
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
            x = { immutableBefore.length },
            y = if (true) { immutableBefore = ""; "" } else { immutableBefore = ""; "" },
        )
        invokeLater(
            y = if (true) { immutableAfter = ""; "" } else { immutableAfter = ""; "" },
            x = { immutableAfter.length },
        )
        invokeLater(
            x = { mutableBefore.length },
            y = if (true) { mutableBefore = ""; "" } else { mutableBefore = ""; "" },
        )
        invokeLater(
            y = if (true) { mutableAfter = ""; "" } else { mutableAfter = ""; "" },
            x = { mutableAfter.length },
        )
    }
}
