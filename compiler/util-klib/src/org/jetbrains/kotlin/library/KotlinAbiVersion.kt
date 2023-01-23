/**
 * Copyright 2010-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.library

fun String.parseKotlinAbiVersion(): KotlinAbiVersion {
    val values = this.split(".").map { it.toInt() }

    return when (values.size) {
        3 -> KotlinAbiVersion(values[0], values[1], values[2])
        1 -> KotlinAbiVersion(values[0])
        else -> error("Could not parse abi version: $this")
    }
}

// TODO: consider inheriting this class from BinaryVersion (but that requires a module structure refactoring.)
//  Advantages: code reuse.
//  Disadvantages: BinaryVersion is a problematic class, because it doesn't represent any logical entity in the codebase, it's just a
//  way to reuse common logic for Kotlin versions. But unfortunately, BinaryVersion is used in a lot of API definitions, which makes
//  code hard to read because it's not obvious which subclasses are supposed to be passed into a particular API.
/**
 * The version of the Kotlin IR.
 *
 * This version must be bumped when:
 * - Incompatible changes are made in `KotlinIr.proto`
 * - Incompatible changes are made in serialization/deserialization logic
 *
 * The version bump must obey [org.jetbrains.kotlin.metadata.deserialization.BinaryVersion] rules (See `BinaryVersion` KDoc)
 */
data class KotlinAbiVersion(val major: Int, val minor: Int, val patch: Int) {
    // For 1.4 compiler we switched klib abi_version to a triple,
    // but we don't break if we still encounter a single digit from 1.3.
    constructor(single: Int) : this(0, single, 0)

    fun isCompatible(): Boolean = isCompatibleTo(CURRENT)

    private fun isCompatibleTo(ourVersion: KotlinAbiVersion): Boolean {
        // Versions before 1.4.1 were the active development phase.
        // Starting with 1.4.1 we are trying to maintain some backward compatibility.
        return if (this.isAtLeast(1, 4, 1))
            major == ourVersion.major && minor <= ourVersion.minor
        else
            this == ourVersion
    }

    fun isAtLeast(version: KotlinAbiVersion): Boolean =
        isAtLeast(version.major, version.minor, version.patch)

    fun isAtLeast(major: Int, minor: Int, patch: Int): Boolean {
        if (this.major > major) return true
        if (this.major < major) return false

        if (this.minor > minor) return true
        if (this.minor < minor) return false

        return this.patch >= patch
    }

    override fun toString() = "$major.$minor.$patch"

    companion object {
        /**
         * # KotlinAbiVersion bump history
         *
         * - Incompatible change in KotlinIr.proto
         *   `1b6a43ba69a 2022-07-21 Vsevolod Tolstopyatov Update IR serialization to reflect changes in IrSyntheticBodyKind for enum entries`
         *   but we can skip abi version bump because we check some other version in different place KT-53620.
         *
         * - Bump 1.6.0 -> 1.7.0
         *   `76da9df1021 2022-05-26 Pavel Kunyavskiy Bump klib ABI version`
         * - The bump is caused by incompatible change in KotlinIr.proto
         *   `d809e260cb1 2021-10-25 Roman Artemev [KLIB] Support `DefinitelyNotNull` type in KLIB`
         *
         * - Bump 1.5.0 -> 1.6.0
         *   `3403c464fe0 2021-05-26 Roman Artemev [KLIB] Promote library ABI version`
         * - The bump is caused by incompatible change in KotlinIr.proto
         *   `6cdac22a23a 2021-05-26 Roman Artemev [IR] Introduce new IdSignatures`
         *
         * - Bump 1.4.2 -> 1.5.0
         *   `caee17fddb9 2021-04-08 Dmitriy Dolovov [IR] Bump ABI version due to string serialization format change`
         * - The bump is caused by string serialization format change in IR
         *   `50326f019b7 2021-03-30 Dmitriy Dolovov [IR] Use the proper encoding for string serialization`
         *
         * - Bump 1.4.1 -> 1.4.2
         *   `eea5a9102c4 2020-11-06 Alexander Gorshenev Bump klib abi version to 1.4.2 to reflect absence of serialized fake overrides`
         * - The bump is caused by stopping serializing overrides. Normally this would be forwards incompatible change and minor version
         *   should have been bumped, not patch version. But since the code supported the case of fake overrides absence from the beginning,
         *   only patch version was bumped
         *   `cb288d47ea5 2020-11-05 Alexander Gorshenev Don't serialize fake overrides anymore`
         *
         * - Bump 1.4.0 -> 1.4.1
         *   `d7226f49522 2020-04-10 Dmitriy Dolovov KLIB. Promote KLIB ABI version`
         * - The bump is caused by
         *   `101442ad14b 2020-04-03 Dmitriy Dolovov KLIB: Store native targets in manifest`
         *   `1b06256650d 2020-04-03 Dmitriy Dolovov KLIB: Add 'native_targets' manifest property`
         *
         * - Bump 0.26.0 -> 1.4.0
         *   `b06a3ea5acb 2020-03-06 Alexander Gorshenev Print out abi version as a full triple`
         * - There is no clear reasons why the bump was needed. I presume that the author was afraid of version format change. The version
         *   was bumped up to 1.4.0 but not to 1.0.0 presumably because of `isVersionRequirementTableWrittenCorrectly` (It checks that
         *   version is at least 1.4) but `isVersionRequirementTableWrittenCorrectly` is invoked only for `BinaryVersion` (and presumably
         *   only for backend specific metadatas, not for IR) => bump up to 1.0.0 was sufficient. Presumably the author wanted to be extra
         *   safe.
         *
         * - ...
         */
        val CURRENT = KotlinAbiVersion(1, 7, 0)
    }
}
