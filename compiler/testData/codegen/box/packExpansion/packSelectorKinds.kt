// IGNORE_BACKEND_K1: ANY

// MODULE: lib
// FILE: lib.kt
package lib

@Target(AnnotationTarget.TYPE)
annotation class Composable

open class ButtonProps(
    val text: String,
    val enabled: Boolean,
    val onClick: () -> String,
    val content: @Composable () -> String,
)

fun Button(
    text: String,
    enabled: Boolean,
    onClick: () -> String,
    content: @Composable () -> String,
): String {
    return "$text|$enabled|${onClick()}|${content()}"
}

fun Button(
    value: Int,
    enabled: Boolean,
    content: @Composable () -> String,
): String {
    return "$value|$enabled|${content()}"
}

val textButton: (text: String, enabled: Boolean, onClick: () -> String, content: @Composable () -> String) -> String = ::Button

open class LabelProps(
    val text: String,
    val enabled: Boolean,
    val modifier: Int,
)

fun RenderLabel(
    text: String,
    enabled: Boolean = true,
    modifier: Int = 0,
): String {
    return "label:$text|$enabled|$modifier"
}

fun RenderLabel(
    value: Int,
    enabled: Boolean = true,
    modifier: Int = 0,
    inlineContent: String = "",
): String {
    return "inline:$value|$enabled|$modifier|$inlineContent"
}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Button
import lib.ButtonProps
import lib.Composable
import lib.LabelProps
import lib.RenderLabel
import lib.textButton

fun TypeAttrs(...ButtonProps.$attrs): String = "$text:$enabled"

fun TypeCallbacks(...ButtonProps.$callbacks): String = onClick()

fun TypeSlots(...ButtonProps.$slots): String = content()

fun FunctionAttrs(...Button.$attrs(textButton)): String = "$text:$enabled"

fun FunctionCallbacks(...Button.$callbacks(textButton)): String = onClick()

fun FunctionSlots(...Button.$slots(textButton)): String = content()

fun ValueAttrs(...textButton.$attrs): String = "$text:$enabled"

fun ValueCallbacks(...textButton.$callbacks): String = onClick()

fun ValueSlots(...textButton.$slots): String = content()

class BoundLabelProps(
    text: String,
    enabled: Boolean,
    modifier: Int,
) : LabelProps(text, enabled, modifier) {
    fun callBoundExclude(): String = RenderLabel(...LabelProps.$props(this).exclude(enabled), enabled = false)
}

class BoundButtonProps(
    text: String,
    enabled: Boolean,
    onClick: () -> String,
    content: @Composable () -> String,
) : ButtonProps(text, enabled, onClick, content) {
    fun callBoundAll(): String = Button(...ButtonProps.$props(this))

    fun callBoundExclude(): String = Button(...ButtonProps.$props(this).exclude(enabled), enabled = false)
}

fun ButtonProps.callBoundExtension(): String = Button(...ButtonProps.$props(this))

fun box(): String {
    val props = ButtonProps(
        text = "type",
        enabled = true,
        onClick = { "tap" },
        content = { "slot" },
    )
    val boundProps = BoundButtonProps(
        text = "bound",
        enabled = true,
        onClick = { "push" },
        content = { "card" },
    )
    val labelProps = BoundLabelProps(
        text = "label",
        enabled = true,
        modifier = 7,
    )

    val results = listOf(
        TypeAttrs(text = props.text, enabled = props.enabled),
        TypeCallbacks(onClick = props.onClick),
        TypeSlots(content = props.content),
        FunctionAttrs(text = "function", enabled = false),
        FunctionCallbacks(onClick = { "press" }),
        FunctionSlots(content = { "panel" }),
        ValueAttrs(text = "value", enabled = true),
        ValueCallbacks(onClick = { "call" }),
        ValueSlots(content = { "body" }),
        labelProps.callBoundExclude(),
        boundProps.callBoundAll(),
        boundProps.callBoundExclude(),
        props.callBoundExtension(),
    )

    val expected = listOf(
        "type:true",
        "tap",
        "slot",
        "function:false",
        "press",
        "panel",
        "value:true",
        "call",
        "body",
        "label:label|false|7",
        "bound|true|push|card",
        "bound|false|push|card",
        "type|true|tap|slot",
    )

    return if (results == expected) "OK" else "fail: $results"
}
