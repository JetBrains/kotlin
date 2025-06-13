// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries
// WITH_STDLIB
import MyEnum.entries

enum class MyEnum

val entries = "local str"

fun test() {
    val s: String = entries
}

/* GENERATED_FIR_TAGS: enumDeclaration, functionDeclaration, localProperty, propertyDeclaration, stringLiteral */
