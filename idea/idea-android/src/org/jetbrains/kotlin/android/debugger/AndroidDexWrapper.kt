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

package org.jetbrains.kotlin.android.debugger

import com.android.dx.cf.direct.DirectClassFile
import com.android.dx.cf.direct.StdAttributeFactory
import com.android.dx.dex.DexOptions
import com.android.dx.dex.cf.CfOptions
import com.android.dx.dex.cf.CfTranslator
import com.android.dx.dex.file.ClassDefItem
import com.android.dx.dex.file.DexFile
import com.intellij.ide.plugins.DynamicallyLoaded
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.ClassToLoad
import java.lang.reflect.Modifier

@DynamicallyLoaded
class AndroidDexWrapper {
    @Suppress("unused") // Used in AndroidOClassLoadingAdapter#dex
    fun dex(classes: Collection<ClassToLoad>): ByteArray? {
        val dexOptions = DexOptions()
        val cfOptions = CfOptions()

        val dexFile = DexFile(dexOptions)

        val methodWithContext = CfTranslator::class.java.declaredMethods
            .singleOrNull { it.name == "translate" && Modifier.isStatic(it.modifiers) && it.parameterCount == 6 }

        val dxContext = methodWithContext?.let { Class.forName("com.android.dx.command.dexer.DxContext").newInstance() }

        for ((_, relativeFileName, bytes) in classes) {
            val cf = DirectClassFile(bytes, relativeFileName, true)
            cf.setAttributeFactory(StdAttributeFactory.THE_ONE)

            val classDef = if (methodWithContext != null) {
                methodWithContext(
                    null,
                    dxContext,
                    cf,
                    bytes,
                    cfOptions,
                    dexOptions,
                    dexFile
                ) as ClassDefItem
            } else {
                CfTranslator.translate(cf, bytes, cfOptions, dexOptions, dexFile)
            }

            dexFile.add(classDef)
        }

        return dexFile.toDex(null, false)
    }
}
