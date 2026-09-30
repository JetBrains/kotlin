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

import kotlin.script.templates.LEGACY_TEMPLATE_API_DEPRECATION_MESSAGE

/**
 * Basic script definition template without parameters
 */
@Deprecated(LEGACY_TEMPLATE_API_DEPRECATION_MESSAGE)
public abstract class SimpleScriptTemplate()

/**
 * Script definition template with standard argv-like parameter; default for regular kotlin scripts
 */
@Deprecated(
    "Use kotlin.script.experimental.templates.ScriptWithArgs from kotlin-scripting-common instead",
    ReplaceWith("ScriptWithArgs", "kotlin.script.experimental.templates.ScriptWithArgs")
)
public abstract class ScriptTemplateWithArgs(val args: Array<String>)

/**
 * Script definition template with generic bindings parameter (String to Object)
 */
@Deprecated(
    "Use kotlin.script.experimental.templates.ScriptWithBindings from kotlin-scripting-common instead",
    ReplaceWith("ScriptWithBindings", "kotlin.script.experimental.templates.ScriptWithBindings")
)
public abstract class ScriptTemplateWithBindings(val bindings: Map<String, Any?>)

