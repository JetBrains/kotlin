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

public trait TypeCapability

public trait Specificity : TypeCapability {

    public enum class Relation {
        LESS_SPECIFIC
        MORE_SPECIFIC
        DONT_KNOW
    }

    public fun getSpecificityRelationTo(otherType: JetType): Relation
}

fun JetType.getSpecificityRelationTo(otherType: JetType) = this.getCapability(javaClass<Specificity>())?.getSpecificityRelationTo(otherType) ?: Specificity.Relation.DONT_KNOW