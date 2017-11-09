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

package org.jetbrains.kotlin.container

import kotlin.reflect.KClass

/*Use to assist injection to provide a default implementation for a certain component and reduce boilerplate in injector code.
* Argument class must be a non-abstract component class or a kotlin object implementing target interface.
* Avoid using when there is no clear 'default' behaviour for a component.
* */
annotation class DefaultImplementation(val impl: KClass<*>)