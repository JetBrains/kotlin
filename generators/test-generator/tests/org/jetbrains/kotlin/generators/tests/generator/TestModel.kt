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

package org.jetbrains.kotlin.generators.tests.generator

import org.jetbrains.kotlin.utils.Printer

interface TestEntityModel {
    val name: String
    val dataString: String?
}

interface TestClassModel : TestEntityModel {
    val innerTestClasses: Collection<TestClassModel>
    val methods: Collection<MethodModel>
    val isEmpty: Boolean
    val dataPathRoot: String?
}

interface MethodModel : TestEntityModel {
    fun shouldBeGenerated(): Boolean = true
    fun generateSignature(p: Printer)
    fun generateBody(p: Printer)
}

interface TestMethodModel : MethodModel {
    override fun generateSignature(p: Printer) {
        p.print("public void $name() throws Exception")
    }
}   
