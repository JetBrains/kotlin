@file:OptIn(ExperimentalJsExport::class)

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
fun greet(name: String): String = "Hello, $name"