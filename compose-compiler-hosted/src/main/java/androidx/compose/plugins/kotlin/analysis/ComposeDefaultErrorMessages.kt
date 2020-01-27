package androidx.compose.plugins.kotlin.analysis

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_COLLECTION_OF_TYPES
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext

object ComposeDefaultErrorMessages : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("Compose")
    override fun getMap() = MAP

    val OUR_STRING_RENDERER = object : DiagnosticParameterRenderer<String> {
        override fun render(obj: String, renderingContext: RenderingContext): String {
            return obj
        }
    }

    init {
        MAP.put(
            ComposeErrors.NO_COMPOSER_FOUND,
            "Couldn't find a valid composer."
        )
        MAP.put(
            ComposeErrors.OPEN_MODEL,
            "Model objects cannot be open or abstract"
        )
        MAP.put(
            ComposeErrors.INVALID_COMPOSER_IMPLEMENTATION,
            "Composer of type ''{0}'' was found to be an invalid Composer implementation. " +
                    "Reason: {1}",
            Renderers.RENDER_TYPE,
            OUR_STRING_RENDERER
        )
        MAP.put(
            ComposeErrors.SUSPEND_FUNCTION_USED_AS_SFC,
            "Suspend functions are not allowed to be used as Components"
        )
        MAP.put(
            ComposeErrors.INVALID_TYPE_SIGNATURE_SFC,
            "Only Unit-returning functions are allowed to be used as Components"
        )
        MAP.put(
            ComposeErrors.COMPOSABLE_INVOCATION_IN_NON_COMPOSABLE,
            "Functions which invoke @Composable functions must be marked with the @Composable " +
                    "annotation"
        )
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