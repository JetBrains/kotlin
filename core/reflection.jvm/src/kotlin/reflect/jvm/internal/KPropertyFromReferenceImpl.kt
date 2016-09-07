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

package kotlin.reflect.jvm.internal

import kotlin.jvm.internal.*

internal open class KProperty0Augmented(
        val reference: PropertyReference0
) : KProperty0Impl<Any?>(reference.owner as KDeclarationContainerImpl, reference.name, reference.signature) {
    override val name: String get() = reference.name
}

internal open class KMutableProperty0Augmented(
        val reference: MutablePropertyReference0
) : KMutableProperty0Impl<Any?>(reference.owner as KDeclarationContainerImpl, reference.name, reference.signature) {
    override val name: String get() = reference.name
}

internal open class KProperty1Augmented(
        val reference: PropertyReference1
) : KProperty1Impl<Any?, Any?>(reference.owner as KDeclarationContainerImpl, reference.name, reference.signature) {
    override val name: String get() = reference.name
}

internal open class KMutableProperty1Augmented(
        val reference: MutablePropertyReference1
) : KMutableProperty1Impl<Any?, Any?>(reference.owner as KDeclarationContainerImpl, reference.name, reference.signature) {
    override val name: String get() = reference.name
}

internal open class KProperty2Augmented(
        val reference: PropertyReference2
) : KProperty2Impl<Any?, Any?, Any?>(reference.owner as KDeclarationContainerImpl, reference.name, reference.signature) {
    override val name: String get() = reference.name
}

internal open class KMutableProperty2Augmented(
        val reference: MutablePropertyReference2
) : KMutableProperty2Impl<Any?, Any?, Any?>(reference.owner as KDeclarationContainerImpl, reference.name, reference.signature) {
    override val name: String get() = reference.name
}
