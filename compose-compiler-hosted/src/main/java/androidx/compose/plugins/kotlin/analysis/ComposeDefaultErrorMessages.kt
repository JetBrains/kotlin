package androidx.compose.plugins.kotlin.analysis

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_COLLECTION_OF_TYPES

object ComposeDefaultErrorMessages : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("Compose")
    override fun getMap() = MAP

    init {
        MAP.put(
            ComposeErrors.ILLEGAL_ASSIGN_TO_UNIONTYPE,
            "Value of type {0} can't be assigned to union type {1}.",
            RENDER_COLLECTION_OF_TYPES,
            RENDER_COLLECTION_OF_TYPES
        )
        MAP.put(
            ComposeErrors.ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE,
            "Try catch is not supported around composable function invocations."
        )
    }
}