// EXCLUDED_CLASSES: /dynamic
// TARGET_BACKEND: JS_IR
// MODULE: specific_types_library

@Suppress("ClassName") class dynamic
fun returnsDynamic(): dynamic = TODO()
fun returnsDynamicClass(): `dynamic` = TODO()
