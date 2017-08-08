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

package org.jetbrains.kotlin.findUsages

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.stubs.createFacet
import org.junit.Test

class FindUsagesMultiModuleTest : AbstractFindUsagesMultiModuleTest() {

    private fun doMultiPlatformTest(commonName: String = "common",
                                    implName: String = "jvm",
                                    implKind: TargetPlatformKind<*> = TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]) {
        val header = module(commonName)
        header.createFacet(TargetPlatformKind.Common)

        val jvm = module(implName)
        jvm.createFacet(implKind)
        jvm.enableMultiPlatform()
        jvm.addDependency(header)

        doFindUsagesTest()
    }

    @Test
    fun testFindImplFromHeader() {
        doMultiPlatformTest()
    }
}