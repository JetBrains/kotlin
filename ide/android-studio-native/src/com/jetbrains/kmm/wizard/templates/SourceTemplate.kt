package com.jetbrains.kmm.wizard.templates

abstract class SourceTemplate(val sourceSet: String, val fileName: String) {
    abstract fun render(packageName: String): String
}