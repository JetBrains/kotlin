package androidx.compose.plugins.kotlin.frames

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import androidx.compose.plugins.kotlin.ComposeUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

internal val composePackageName = FqName(ComposeUtils.generateComposePackageName())
internal val framesPackageName = composePackageName.child(Name.identifier("frames"))
internal val abstractRecordClassName = framesPackageName.child(Name.identifier("AbstractRecord"))
internal val recordClassName = framesPackageName.child(Name.identifier("Record"))
internal val componentClassName = composePackageName.child(Name.identifier("Component"))
internal val modelClassName = composePackageName.child(Name.identifier("Model"))
internal val framedTypeName = framesPackageName.child(Name.identifier("Framed"))
internal fun ClassDescriptor.isFramed(): Boolean =
    getSuperClassNotAny()?.fqNameSafe == componentClassName
internal fun ModuleDescriptor.findTopLevel(name: FqName) =
    findClassAcrossModuleDependencies(ClassId.topLevel(name)) ?: error("Could not find $name")
