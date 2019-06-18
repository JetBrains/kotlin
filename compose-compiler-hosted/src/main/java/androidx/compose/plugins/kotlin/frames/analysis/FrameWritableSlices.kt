package androidx.compose.plugins.kotlin.frames.analysis

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import androidx.compose.plugins.kotlin.frames.FrameRecordClassDescriptor
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object FrameWritableSlices {
    val RECORD_CLASS: WritableSlice<FqName, FrameRecordClassDescriptor> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val FRAMED_DESCRIPTOR: WritableSlice<FqName, ClassDescriptor> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}