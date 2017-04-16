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

/**
 *  https://en.wikipedia.org/wiki/Software_versioning
 *  scheme major.minor[.build[.revision]].
*/

enum class MetaVersion {
  EAP,
  ALPHA,
  BETA,
  RC
}

class KonanVersion(val meta: MetaVersion?, val major: Int, val minor: Int, val maintenance: Int, val build:Int) {
    companion object {
        val CURRENT = KonanVersion(MetaVersion.EAP, 0, 1, 0, 0)
    }
    override fun toString() = if (meta != null) "$meta $major.$minor.$maintenance.$build" else "$major.$minor.$maintenance.$build"
}
