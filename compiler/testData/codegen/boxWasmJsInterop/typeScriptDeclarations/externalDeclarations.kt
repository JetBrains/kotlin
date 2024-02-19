// CHECK_TYPESCRIPT_DECLARATIONS
// TARGET_BACKEND: WASM
// MODULE: main

// FILE: first.kt
external object MentionedParent {
    val value: String
    interface Nested {
        val value: Int
    }
}

external class Parent {
    object OneMoreLayer {
        interface MentionedNested {
            val value: MentionedParent
        }
    }
    companion object {
        class AnotherMentionedNested {
            val value: String
        }
    }
}

external interface Bar {
    val bar: Parent.OneMoreLayer.MentionedNested
    val oneMore: Parent.Companion.AnotherMentionedNested
}

external interface Baz<T: JsAny?> : Bar {
    val baz: T
}

// FILE: second.kt
package org.second

import Bar
import Baz

external object Foo : Baz<JsString> {
    override val bar: Parent.OneMoreLayer.MentionedNested
    override val baz: JsString
    override val oneMore: Parent.Companion.AnotherMentionedNested
}

external abstract class BaseResult<T: JsAny>(foo: Foo)

external class Result<T: JsAny> : BaseResult<T>

fun getResultInternal(): Result<JsString> = js("({})")

@JsExport
fun getResult(): Result<JsString> = getResultInternal()

// FILE: entry.mjs

import { getResult } from "./index.mjs";

if (JSON.stringify(getResult()) != "{}") throw new Error("Unexpected result")