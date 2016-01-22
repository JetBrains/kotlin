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

package kotlin.reflect.jvm

import java.lang.reflect.Field
import kotlin.reflect.*

@Deprecated("Use kotlinProperty instead.", ReplaceWith("kotlinProperty")) val Field.kotlin: KProperty<*>?
    get() = kotlinProperty


@Deprecated("Use memberProperties instead.", ReplaceWith("memberProperties")) val <T : Any> KClass<T>.properties: Collection<KProperty1<T, *>>
    get() = memberProperties

@Deprecated("Use extensionProperties instead.", ReplaceWith("extensionProperties")) val <T : Any> KClass<T>.extensionProperties: Collection<KProperty2<T, *, *>>
    get() = memberExtensionProperties

@Deprecated("Use declaredMemberProperties instead.", ReplaceWith("declaredMemberProperties")) val <T : Any> KClass<T>.declaredProperties: Collection<KProperty1<T, *>>
    get() = declaredMemberProperties

@Deprecated("Use declaredMemberExtensionProperties instead.", ReplaceWith("declaredMemberExtensionProperties")) val <T : Any> KClass<T>.declaredExtensionProperties: Collection<KProperty2<T, *, *>>
    get() = declaredMemberExtensionProperties


@Deprecated("Use isAccessible instead.", ReplaceWith("isAccessible")) var KProperty<*>.accessible: Boolean
    get() = isAccessible
    set(value) { isAccessible = value }

@Deprecated("Moved to package kotlin.jvm", ReplaceWith("java"), level = DeprecationLevel.HIDDEN) val <T : Any> KClass<T>.java: Class<T>
    get() = java

@Deprecated("Moved to package kotlin.jvm", ReplaceWith("kotlin"), level = DeprecationLevel.HIDDEN) val <T : Any> Class<T>.kotlin: KClass<T>
    get() = kotlin
