// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-65581

typealias Aliased = Int
annotation class Tag(vararg val tags: Aliased)
