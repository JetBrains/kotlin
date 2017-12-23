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

package org.jetbrains.kotlin.idea.framework

import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon

class JSFrameworkType : FrameworkTypeEx("kotlin-js-framework-id") {

    override fun createProvider(): FrameworkSupportInModuleProvider = JSFrameworkSupportProvider()

    override fun getPresentableName() = "Kotlin/\u200BJS"

    override fun getIcon(): Icon = KotlinIcons.JS

    companion object {
        val instance: JSFrameworkType
            get() = FrameworkTypeEx.EP_NAME.findExtension(JSFrameworkType::class.java)
    }
}
