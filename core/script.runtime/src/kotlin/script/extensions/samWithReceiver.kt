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

package kotlin.script.extensions

// discuss

// Is this an appropriate place to put this class?
// Used like this: https://github.com/gradle/kotlin-dsl/blob/cb44112374e36b41732ab390531b8bc29e8de327/provider/src/main/kotlin/org/gradle/kotlin/dsl/KotlinBuildScript.kt#L35
// Provides access to some specific compiler extension in scripts.
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SamWithReceiverAnnotations(vararg val annotations: String)