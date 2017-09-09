/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.getSinceKotlinAnnotation

private val kotlin: FqName = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
private val kotlinText: FqName = KotlinBuiltIns.TEXT_PACKAGE_FQ_NAME
private val kotlinCollections: FqName = KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME
private val kotlinStreams: FqName = kotlin.child(Name.identifier("streams"))

private val use: Name = Name.identifier("use")
private val get: Name = Name.identifier("get")
private val getOrDefault: Name = Name.identifier("getOrDefault")
private val remove: Name = Name.identifier("remove")

// We treat all declarations in deprecated libraries kotlin-stdlib-jre7 and kotlin-stdlib-jre8 as if they were
// annotated with @LowPriorityInOverloadResolution.
// This is needed in order to avoid reporting "overload resolution ambiguity" in case when there's both kotlin-stdlib-jreN and
// kotlin-stdlib-jdkN in dependencies, to allow smooth migration from the former to the latter. The ambiguity here would be incorrect
// because there's really no ambiguity in the bytecode since all declarations in -jdk7/8 have been moved to another JVM package.
// We locate declarations in kotlin-stdlib-jre{7,8} by FQ name and @SinceKotlin annotation value, which should be 1.1.
fun CallableDescriptor.isLowPriorityFromStdlibJre7Or8(): Boolean {
    val containingPackage = containingDeclaration as? PackageFragmentDescriptor ?: return false
    val packageFqName = containingPackage.fqName
    if (!packageFqName.startsWith(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) return false

    val isFromStdlibJre7Or8 =
            packageFqName == kotlin && name == use ||
            packageFqName == kotlinText && name == get ||
            packageFqName == kotlinCollections && (name == getOrDefault || name == remove) ||
            packageFqName == kotlinStreams

    if (!isFromStdlibJre7Or8) return false

    val sinceKotlin = getSinceKotlinAnnotation() ?: return false
    val version = sinceKotlin.allValueArguments.values.singleOrNull()?.value as? String ?: return false

    return version == LanguageVersion.KOTLIN_1_1.versionString
}
