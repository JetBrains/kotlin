/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import java.io.InputStream

public class BuiltInsClassDataFinder(
        private val packageFragmentProvider: PackageFragmentProvider,
        private val loadResource: (path: String) -> InputStream?
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassData? {
        val packageFragment = packageFragmentProvider.getPackageFragments(classId.getPackageFqName()).singleOrNull() ?: return null

        val stream = loadResource(BuiltInsSerializationUtil.getClassMetadataPath(classId)) ?: return null

        val classProto = ProtoBuf.Class.parseFrom(stream, BuiltInsSerializationUtil.EXTENSION_REGISTRY)
        val nameResolver =
                (packageFragment as? BuiltinsPackageFragment ?: error("Not a built-in package fragment: $packageFragment")).nameResolver

        val expectedShortName = classId.getShortClassName()
        val actualShortName = nameResolver.getClassId(classProto.getFqName()).getShortClassName()
        if (!actualShortName.isSpecial() && actualShortName != expectedShortName) {
            // Workaround for case-insensitive file systems,
            // otherwise we'd find "Collection" for "collection" etc
            return null
        }

        return ClassData(nameResolver, classProto)
    }
}
