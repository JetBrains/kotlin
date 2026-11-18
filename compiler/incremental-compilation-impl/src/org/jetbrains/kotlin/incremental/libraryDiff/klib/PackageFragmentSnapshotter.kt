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
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.getClassId

internal object PackageFragmentSnapshotter {

    /**
     * Returns the snapshots of all package fragments in [metadata].
     *
     * Fragments of the same package that come from the same source file are merged into one snapshot.
     */
    fun snapshot(metadata: KlibMetadataComponent, settings: ClasspathEntrySnapshotter.Settings): Set<PackageFragmentSnapshot> {
        val snapshots = LinkedHashSet<PackageFragmentSnapshot>()

        for (packageName in metadata.getPackageNames()) {
            val packageFqName = FqName(packageName)
            val builders = LinkedHashMap<String, PackageFragmentSnapshotBuilder>()

            for (fragmentName in metadata.getPackageFragmentNames(packageName)) {
                val bytes = metadata.getPackageFragment(packageName, fragmentName)
                val fragment = parsePackageFragment(bytes)

                val nameResolver = NameResolverImpl(fragment.strings, fragment.qualifiedNames)
                val sourceFile = fragment.sourceFileName(nameResolver)

                val classInfos = getClassInfos(fragment, nameResolver)
                val packageMembers = PackagePartProtoData(fragment.`package`, nameResolver, packageFqName).getNonPrivateMembers()

                builders.getOrPut(sourceFile) { PackageFragmentSnapshotBuilder(packageFqName, sourceFile) }.apply {
                    this.classInfos.addAll(classInfos)
                    this.packageMemberNames.addAll(packageMembers)
                    this.fragmentsBytes.add(bytes)
                }
            }

            builders.values.mapTo(snapshots) { it.build(settings) }
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

    // TODO(KT-91548): read it from the fragment itself once it is stored at the package fragment level.
    private fun ProtoBuf.PackageFragment.sourceFileName(nameResolver: NameResolver): String {
        val fileId = class_List.firstNotNullOfOrNull { it.getExtensionOrNull(KlibMetadataProtoBuf.classFile) }
            ?: `package`.functionList.firstNotNullOfOrNull { it.getExtensionOrNull(KlibMetadataProtoBuf.functionFile) }
            ?: `package`.propertyList.firstNotNullOfOrNull { it.getExtensionOrNull(KlibMetadataProtoBuf.propertyFile) }
            ?: `package`.typeAliasList.firstNotNullOfOrNull { it.getExtensionOrNull(KlibMetadataProtoBuf.typeAliasFile) }
        val fileName = fileId?.let { PackagePartClassUtils.getFilePartShortName(nameResolver.getString(it)) }

        return fileName ?: "<package>"
    }

    /**
     * A class is accessible if it is not private and its outer class, if any, is accessible (like [InaccessibleClassSnapshot] on the JVM).
     * Inaccessible classes can't be referenced from other modules. Nested classes are always in the same fragment as their outer class,
     * so [classes] holds all classes of the fragment.
     */
    private fun ClassId.isAccessibleIn(classes: Map<ClassId, ProtoBuf.Class>): Boolean {
        val visibility = Flags.VISIBILITY.get(classes.getValue(this).flags)
        if (visibility == ProtoBuf.Visibility.PRIVATE || visibility == ProtoBuf.Visibility.PRIVATE_TO_THIS) {
            return false
        }

        val outerClassId = outerClassId ?: return true
        return outerClassId !in classes || outerClassId.isAccessibleIn(classes)
    }
}

private class PackageFragmentSnapshotBuilder(private val packageName: FqName, private val sourceFile: String) {
    val fragmentsBytes = mutableListOf<ByteArray>()
    val classInfos = mutableListOf<KlibClassInfo>()
    val packageMemberNames = mutableSetOf<String>()

    fun build(settings: ClasspathEntrySnapshotter.Settings): PackageFragmentSnapshot {
        check(fragmentsBytes.isNotEmpty()) { "No package fragments were added for $sourceFile in package $packageName" }

        return PackageFragmentSnapshot(
            id = getId(packageName, sourceFile),
            packageName = packageName,
            contentHash = fragmentsBytes.map { it.hashToLong() }.reduce { acc, hash -> acc * 31 + hash },
            classInfos = classInfos.toList(),
            packageMemberNames = packageMemberNames.toSet(),
            packageFragmentProto = fragmentsBytes.toList().takeIf { settings.granularity == CLASS_MEMBER_LEVEL },
        )
    }

    private fun getId(packageName: FqName, sourceFile: String) =
        if (packageName.isRoot) sourceFile else "${packageName.asString().replace('.', '/')}/$sourceFile"
}
