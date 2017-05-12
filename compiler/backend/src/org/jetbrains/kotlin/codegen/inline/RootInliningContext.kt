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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.psi.KtElement

class RootInliningContext(
        expressionMap: Map<Int, LambdaInfo>,
        state: GenerationState,
        nameGenerator: NameGenerator,
        val callElement: KtElement,
        override val callSiteInfo: InlineCallSiteInfo,
        inliner: ReifiedTypeInliner,
        val typeParameterMappings: TypeParameterMappings
) : InliningContext(null, expressionMap, state, nameGenerator, TypeRemapper.createRoot(typeParameterMappings), inliner, false, false)
