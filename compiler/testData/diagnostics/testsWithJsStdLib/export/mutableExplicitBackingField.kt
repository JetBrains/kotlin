// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ExplicitBackingFields
// WITH_STDLIB
@file:OptIn(kotlin.js.ExperimentalJsCollectionsApi::class)

import kotlin.js.collections.JsArray
import kotlin.js.collections.JsReadonlyArray

@JsExport
val items: JsReadonlyArray<String>
    field: JsArray<String> = JsArray()
