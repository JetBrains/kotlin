package androidx.compose.plugins.kotlin.analysis

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import androidx.compose.plugins.kotlin.ComposableAnnotationChecker
import androidx.compose.plugins.kotlin.ResolvedKtxElementCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object ComposeWritableSlices {
    val COMPOSABLE_ANALYSIS: WritableSlice<KtElement, ComposableAnnotationChecker.Composability> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val RESOLVED_KTX_CALL: WritableSlice<KtElement, ResolvedKtxElementCall> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val ATTRIBUTE_KEY_REFERENCE_TARGET:
            WritableSlice<KtReferenceExpression, Set<DeclarationDescriptor>> =
        BasicWritableSlice(REWRITES_ALLOWED)
    val FAILED_CANDIDATES: WritableSlice<KtElement, Collection<ResolvedCall<FunctionDescriptor>>> =
        BasicWritableSlice(REWRITES_ALLOWED)
    val FCS_CALL_WITHIN_COMPOSABLE: WritableSlice<KtElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val FCS_RESOLVEDCALL_COMPOSABLE: WritableSlice<KtElement, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val INFERRED_COMPOSABLE_DESCRIPTOR: WritableSlice<FunctionDescriptor, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}

private val REWRITES_ALLOWED = object : RewritePolicy {
    override fun <K : Any?> rewriteProcessingNeeded(key: K): Boolean = true
    override fun <K : Any?, V : Any?> processRewrite(
        slice: WritableSlice<K, V>?,
        key: K,
        oldValue: V,
        newValue: V
    ): Boolean = true
}
