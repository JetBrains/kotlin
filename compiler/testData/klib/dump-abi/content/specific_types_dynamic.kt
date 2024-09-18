// EXCLUDED_CLASSES: /dynamic
// TARGET_BACKEND: JS
// MODULE: specific_types_library

@Suppress("ClassName") class dynamic
fun returnsDynamic(): dynamic = TODO()
fun returnsDynamicClass(): `dynamic` = TODO()
