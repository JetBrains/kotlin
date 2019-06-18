/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This package contains all the logic related to lowering a PSI containing Compose Components into IR.
 * The entry-point for this package is ComponentClassLowering, which will generate all supporting
 * synthetics.
 * Each synthetic class of type [ClassName] lives in a file called [ClassName]Generator.
 *
 * Anything beginning with the token `lower` may modify IR
 * Anything beginning with the token `generate` may only produce (return) IR
 */
package androidx.compose.plugins.kotlin.compiler.lower;
