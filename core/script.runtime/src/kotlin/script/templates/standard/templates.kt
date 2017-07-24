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

package kotlin.script.templates.standard

// discuss
//
// These are some 'basic' script templates
// Should we keep them here?
/**
 * Basic script definition template without parameters
 */
// didn't find any usages of this class.
public abstract class SimpleScriptTemplate()

/**
 * Script definition template with standard argv-like parameter; default for regular kotlin scripts
 */
// Used here: https://github.com/JetBrains/kotlin/blob/65dba3615c2699e35e8da65850efc97afd674ad5/compiler/frontend/src/org/jetbrains/kotlin/script/KotlinScriptDefinition.kt#L50-L50
public abstract class ScriptTemplateWithArgs(val args: Array<String>)

/**
 * Script definition template with generic bindings parameter (String to Object)
 */
// Used here: https://github.com/JetBrains/kotlin/blob/f152af6385b2a5df8e598f2a604b1e66114f860e/idea/idea-repl/src/org/jetbrains/kotlin/jsr223/KotlinJsr223StandardScriptEngineFactory4Idea.kt#L38-L38
public abstract class ScriptTemplateWithBindings(val bindings: Map<String, Any?>)

