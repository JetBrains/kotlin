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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.util.*

enum class KonanPhase(val description: String,
                      vararg prerequisite: KonanPhase,
                      var enabled: Boolean = true, var verbose: Boolean = false) {

    /* */ FRONTEND("Frontend builds AST"),
    /* */ PSI_TO_IR("Psi to IR conversion"),
    /* */ SERIALIZER("Serialize descriptor tree and inline IR bodies"),
    /* */ BACKEND("All backend"),
    /* ... */ LOWER("IR Lowering"),
    /* ... ... */ LOWER_INLINE("Functions inlining"),
    /* ... ... ...  */ DESERIALIZER("Deserialize inline bodies"),
    /* ... ... */ LOWER_INTEROP("Interop lowering"),
    /* ... ... */ LOWER_ENUMS("Enum classes lowering"),
    /* ... ... */ LOWER_DELEGATION("Delegation lowering"),
    /* ... ... */ LOWER_INITIALIZERS("Initializers lowering", LOWER_ENUMS),
    /* ... ... */ LOWER_SHARED_VARIABLES("Shared Variable Lowering", LOWER_INITIALIZERS),
    /* ... ... */ LOWER_CALLABLES("Callable references Lowering",
                        LOWER_INTEROP, LOWER_INITIALIZERS, LOWER_DELEGATION),
    /* ... ... */ LOWER_VARARG("Vararg lowering", LOWER_CALLABLES),
    /* ... ... */ LOWER_LOCAL_FUNCTIONS("Local Function Lowering", LOWER_SHARED_VARIABLES),
    /* ... ... */ LOWER_TAILREC("tailrec lowering", LOWER_LOCAL_FUNCTIONS),
    /* ... ... */ LOWER_DEFAULT_PARAMETER_EXTENT("Default Parameter Extent Lowering",
                        LOWER_TAILREC, LOWER_ENUMS),
    /* ... ... */ LOWER_INNER_CLASSES("Inner classes lowering", LOWER_DEFAULT_PARAMETER_EXTENT),
    /* ... ... */ LOWER_LATEINIT("Lateinit properties lowering"),
    /* ... ... */ LOWER_BUILTIN_OPERATORS("BuiltIn Operators Lowering", LOWER_DEFAULT_PARAMETER_EXTENT, LOWER_LATEINIT),
    /* ... ... */ LOWER_TYPE_OPERATORS("Type operators lowering"),
    /* ... ... */ BRIDGES_BUILDING("Bridges building"),
    /* ... ... */ LOWER_STRING_CONCAT("String concatenation lowering"),
    /* ... ... */ AUTOBOX("Autoboxing of primitive types", BRIDGES_BUILDING),
    /* ... */ BITCODE("LLVM BitCode Generation"),
    /* ... ... */ RTTI("RTTI Generation"),
    /* ... ... */ CODEGEN("Code Generation"),
    /* ... ... */ METADATOR("Metadata Generation"),
    /* ... ... */ BITCODE_LINKER("Bitcode linking"),
    /* */ LINK_STAGE("Link stage"),
    /* ... */ OBJECT_FILES("Bitcode to object file"),
    /* ... */ LINKER("Linker");

    val prerequisite = prerequisite.toSet()
}

object KonanPhases {
    val phases = KonanPhase.values().associate { it.name.toLowerCase() to it }

    fun known(name: String): String {
        if (phases[name] == null) {
            error("Unknown phase: $name. Use --list_phases to see the list of phases.")
        }
        return name
    }

    fun config(config: KonanConfig) {
        with (config.configuration) { with (KonanConfigKeys) { 

            // Don't serialize anything to a final executable.
            KonanPhase.SERIALIZER.enabled = getBoolean(NOLINK)

            val disabled = get(DISABLED_PHASES)
            disabled?.forEach { phases[known(it)]!!.enabled = false }

            val enabled = get(ENABLED_PHASES)
            enabled?.forEach { phases[known(it)]!!.enabled = true }

            val verbose = get(VERBOSE_PHASES)
            verbose?.forEach { phases[known(it)]!!.verbose = true }

            if (get(NOLINK) ?: false ) {
                KonanPhase.LINK_STAGE.enabled = false
            }
        }}
    }

    fun list() {
        phases.forEach { key, phase ->
            val enabled = if (phase.enabled) "(Enabled)" else ""
            val verbose = if (phase.verbose) "(Verbose)" else ""
            
            println(String.format("%1$-30s%2$-30s%3$-10s", "${key}:", phase.description, "$enabled $verbose"))
        }
    }
}

internal class PhaseManager(val context: Context)  {

    val previousPhases = mutableSetOf<KonanPhase>()

    internal fun phase(phase: KonanPhase, body: () -> Unit) {

        if (!phase.enabled) return

        phase.prerequisite.forEach {
            if (!previousPhases.contains(it))
                throw Error("$phase requires $it")
        }

        previousPhases.add(phase)

        val savePhase = context.phase
        context.phase = phase
        context.depth ++

        with (context) {
            profileIf(shouldProfilePhases(), "Phase ${nTabs(depth)} ${phase.name}") {
                body() 
            }

            if (shouldVerifyDescriptors()) {
                verifyDescriptors()
            }
            if (shouldVerifyIr()) {
                verifyIr()
            }

            if (shouldPrintDescriptors()) {
                printDescriptors()
            }
            if (shouldPrintIr()) {
                printIr()
            }
            if (shouldPrintIrWithDescriptors()) {
                printIrWithDescriptors()
            }
            if (shouldPrintLocations()) {
                printLocations()
            }
        }

        context.depth --
        context.phase = savePhase
    }
}


