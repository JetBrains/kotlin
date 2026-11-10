@file:OptIn(ExperimentalJsExport::class)

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
fun greet(name: String): String = "Hello, $name"

@JsExport
fun takeAny(x: Any): Any = x

@JsExport
fun takeDynamic(x: dynamic): dynamic = x