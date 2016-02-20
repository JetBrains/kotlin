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

package org.jetbrains.kotlin.preprocessor

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtUserType

interface Conditional {

    interface PlatformVersion : Conditional

    abstract class Parser(val name: String, val parse: (arguments: PositionalAndNamedArguments) -> Conditional)

    data class JvmVersion(val minimum: Int, val maximum: Int): PlatformVersion {
        val versionRange: IntRange = minimum..maximum
        companion object : Parser("JvmVersion", parse = { arguments ->
            val minimum = arguments[0, "minimum"]?.parseIntegerValue()
            val maximum = arguments[1, "maximum"]?.parseIntegerValue()

            JvmVersion(minimum ?: 6, maximum ?: 100)
        })
    }

    data class JsVersion(val version: Int = 5): PlatformVersion {
        companion object : Parser("JsVersion", parse = { JsVersion() })
    }

    data class TargetName(val name: String): Conditional {
        companion object : Parser("RenameOnTargetPlatform", parse = { arguments ->
            val name = arguments[0, "name"]!!.parseStringValue()
            TargetName(name)
        })
    }


    companion object {
        val ANNOTATIONS: Map<String, Parser> = listOf<Parser>(JvmVersion, JsVersion, TargetName).associateBy { it.name }
    }
}

fun KtAnnotated.parseConditionalAnnotations(): List<Conditional> =
        annotationEntries.mapNotNull {
            val parser = Conditional.ANNOTATIONS.get(it.typeReferenceName)
            parser?.parse?.invoke(it.valueArguments.splitToPositionalAndNamed())
        }


val KtAnnotationEntry.typeReferenceName: String? get() =
        (typeReference?.typeElement as? KtUserType)?.referencedName

