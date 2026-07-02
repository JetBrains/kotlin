// WITH_STDLIB
// LANGUAGE: +AllowAnyAsAnActualTypeForExpectInterface

// kotlin-wrappers/kotlin-cssom-core/src/webMain/generated/web/cssom/Flex.kt

sealed external interface FlexGrow: JsAny

sealed external interface FlexShrink: JsAny

// for WasmJs need JsAny
sealed external interface FlexBasis: JsAny {
    companion object
}

inline val FlexBasis.Companion.content: FlexBasis get() = "content".toJsString().unsafeCast<FlexBasis>()

inline val FlexBasis.Companion.fitContent: FlexBasis get() = "fit-content".toJsString().unsafeCast<FlexBasis>()

inline val FlexBasis.Companion.maxContent: FlexBasis get() = "max-content".toJsString().unsafeCast<FlexBasis>()

inline val FlexBasis.Companion.minContent: FlexBasis get() = "min-content".toJsString().unsafeCast<FlexBasis>()


//from kotlin-wrappers
//
//sealed external interface Flex {
//    companion object {
//        @JsValue("content")
//        val content: Flex
//
//        @JsValue("fit-content")
//        val fitContent: Flex
//
//        @JsValue("max-content")
//        val maxContent: Flex
//
//        @JsValue("min-content")
//        val minContent: Flex
//    }
//}

sealed external interface Flex: JsAny {
    companion object
}

inline val Flex.Companion.content: Flex get() = "content".toJsString().unsafeCast<Flex>()

inline val Flex.Companion.fitContent: Flex get() = "fit-content".toJsString().unsafeCast<Flex>()

inline val Flex.Companion.maxContent: Flex get() = "max-content".toJsString().unsafeCast<Flex>()

inline val Flex.Companion.minContent: Flex get() = "min-content".toJsString().unsafeCast<Flex>()

inline fun Flex(
    grow: FlexGrow,
    basis: FlexBasis,
): Flex =
    "$grow $basis".toJsString().unsafeCast<Flex>()

inline fun Flex(
    grow: FlexGrow,
    shrink: FlexShrink,
): Flex =
    "$grow $shrink".toJsString().unsafeCast<Flex>()

inline fun Flex(
    grow: FlexGrow,
    shrink: FlexShrink,
    basis: FlexBasis,
): Flex =
    "$grow $shrink $basis".toJsString().unsafeCast<Flex>()

fun keywordConstants() {
  assertEquals("content",     Flex.Companion.content.toString())
  assertEquals("fit-content", Flex.Companion.fitContent.toString())
  assertEquals("max-content", Flex.Companion.maxContent.toString())
  assertEquals("min-content", Flex.Companion.minContent.toString())
}

fun composite() {
  val grow: FlexGrow = 1.toJsReference().unsafeCast<FlexGrow>()
  val shrink: FlexShrink = 0.toJsReference().unsafeCast<FlexShrink>()
  assertEquals("1 0",      Flex(grow, shrink).toString())
  assertEquals("1 0 auto", Flex(grow, shrink, FlexBasis.Companion.fitContent.let { "auto".toJsString().unsafeCast<FlexBasis>() }).toString())
}

//fun appliedToStyle() {
//  val style = document.createElement("div").unsafeCast<HTMLElement>().style
//  style.setProperty("flex", Flex.fitContent.toString())
//  assertEquals("fit-content", style.getPropertyValue("flex"))
//}

external interface Flush {
    companion object {
        val Z_NO_FLUSH: Flush
        val Z_PARTIAL_FLUSH: Flush
        val Z_SYNC_FLUSH: Flush
        val Z_FINISH: Flush
        val Z_BLOCK: Flush
        val Z_TREES: Flush
    }
}

fun box(): String {
    keywordConstants()
    composite()

    return "OK"
}
