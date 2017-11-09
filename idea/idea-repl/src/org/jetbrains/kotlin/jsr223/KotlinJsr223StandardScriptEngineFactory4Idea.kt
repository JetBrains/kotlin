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

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.util.scriptCompilationClasspathFromContext
import org.jetbrains.kotlin.utils.PathUtil
import javax.script.ScriptContext
import javax.script.ScriptEngine

@Suppress("unused") // used in javax.script.ScriptEngineFactory META-INF file
class KotlinJsr223StandardScriptEngineFactory4Idea : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmScriptEngine4Idea(
                    this,
                    scriptCompilationClasspathFromContext(PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR),
                    "kotlin.script.templates.standard.ScriptTemplateWithBindings",
                    { ctx, argTypes -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), argTypes ?: emptyArray()) },
                    arrayOf(Map::class)
            )
}

