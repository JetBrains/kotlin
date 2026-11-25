/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacade.Companion.getNonPrivateMembers
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.jetbrains.kotlin.incremental.classpathDiff.InaccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.impl.hashToLong
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.getClassId

internal object KlibFileSnapshotter {

    /**
     * Returns one snapshot per package fragment in [metadata].
     */
    fun snapshot(metadata: KlibMetadataComponent, settings: ClasspathEntrySnapshotter.Settings): List<KlibFileSnapshot> {
        val snapshots = mutableListOf<KlibFileSnapshot>()

        for (packageName in metadata.getPackageNames()) {
            val packageFqName = FqName(packageName)

            for (fragmentName in metadata.getPackageFragmentNames(packageName)) {
                val bytes = metadata.getPackageFragment(packageName, fragmentName)
                val fragment = parsePackageFragment(bytes)
                val nameResolver = NameResolverImpl(fragment.strings, fragment.qualifiedNames)
                val packageMembers = PackagePartProtoData(fragment.`package`, nameResolver, packageFqName).getNonPrivateMembers()
                val klibFileInfo = KlibFileInfo(packageFqName, bytes)

                snapshots += KlibFileSnapshot(
                    packageName = packageFqName,
                    contentHash = KlibFileInfoExternalizer.toByteArray(klibFileInfo).hashToLong(),
                    classInfos = getClassInfos(fragment, nameResolver),
                    packageMemberNames = packageMembers.toSet(),
                    classMemberLevelSnapshot = klibFileInfo.takeIf { settings.granularity == CLASS_MEMBER_LEVEL },
                )
            }
        }

        return snapshots
    }

    private fun getClassInfos(fragment: ProtoBuf.PackageFragment, nameResolver: NameResolver): List<KlibClassInfo> {
        val accessibleClasses = fragment.class_List
            .associateBy { nameResolver.getClassId(it.fqName) }
            .let { classes -> classes.filterKeys { it.isAccessibleIn(classes) } }

        return accessibleClasses.map { [classId, classProto] ->
            KlibClassInfo(
                classId = classId,
                supertypes = classProto.supertypes(TypeTable(classProto.typeTable))
                    .filter { it.hasClassName() }
                    .map { nameResolver.getClassId(it.className) },
            )
        }
    }
}

/**
 * A class is accessible if it is not private and its outer class, if any, is accessible (like [InaccessibleClassSnapshot] on the JVM).
 * Inaccessible classes can't be referenced from other modules. Nested classes are always in the same fragment as their outer class,
 * so [classes] holds all classes of the fragment.
 */
internal fun ClassId.isAccessibleIn(classes: Map<ClassId, ProtoBuf.Class>): Boolean {
    val visibility = Flags.VISIBILITY.get(classes.getValue(this).flags)
    if (visibility == ProtoBuf.Visibility.PRIVATE || visibility == ProtoBuf.Visibility.PRIVATE_TO_THIS) {
        return false
    }

    val outerClassId = outerClassId ?: return true
    return outerClassId !in classes || outerClassId.isAccessibleIn(classes)
}
