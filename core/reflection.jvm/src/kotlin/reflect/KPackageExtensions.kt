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

package kotlin.reflect

import kotlin.reflect.jvm.internal.KPackageImpl

/**
 * Returns all non-extension properties declared in this package.
 */
public val KPackage.properties: Collection<KProperty0<*>>
    get() = (this as KPackageImpl)
            .getMembers(scope, declaredOnly = false, nonExtensions = true, extensions = false)
            .filterIsInstance<KProperty0<*>>()
            .toList()

/**
 * Returns all extension properties declared in this package.
 */
public val KPackage.extensionProperties: Collection<KProperty1<*, *>>
    get() = (this as KPackageImpl)
            .getMembers(scope, declaredOnly = false, nonExtensions = false, extensions = true)
            .filterIsInstance<KProperty1<*, *>>()
            .toList()
