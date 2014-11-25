/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types

import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

open class DynamicTypesSettings {
    open val dynamicTypesAllowed: Boolean
        get() = false
}

class DynamicTypesAllowed: DynamicTypesSettings() {
    override val dynamicTypesAllowed: Boolean
        get() = true
}

object DynamicType : DelegatingFlexibleType(
        KotlinBuiltIns.getInstance().getNothingType(),
        KotlinBuiltIns.getInstance().getNullableAnyType(),
        FlexibleTypeCapabilities.NONE
) {
    override fun getDelegate() = upperBound
}