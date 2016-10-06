/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jsr223

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.utils.PathUtil
import javax.script.ScriptContext
import javax.script.ScriptEngine

@Suppress("unused") // used in javax.script.ScriptEngineFactory META-INF file
class KotlinJsr223StandardScriptEngineFactory4Idea : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmScriptEngine4Idea(
                    Disposer.newDisposable(),
                    this,
                    listOf(PathUtil.getKotlinPathsForIdeaPlugin().runtimePath),
                    "kotlin.script.ScriptTemplateWithArgsAndBindings",
                    { ctx ->
                        val bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE)
                        arrayOf(
                                (bindings[ScriptEngine.ARGV] as? Array<*>) ?: emptyArray<String>(),
                                bindings) },
                    arrayOf(Array<String>::class.java, Map::class.java)
            )
}
