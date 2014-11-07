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

package kotlin.reflect.jvm.internal

import kotlin.reflect.*

fun <T> kClass(jClass: Class<T>): KClassImpl<T> =
        KClassImpl<T>(jClass, false)

fun <T> kClassFromKotlin(jClass: Class<T>): KClassImpl<T> =
        KClassImpl<T>(jClass, true)

fun kPackage(jClass: Class<*>): KPackageImpl =
        KPackageImpl(jClass)

fun topLevelVariable(name: String, owner: KPackageImpl): KTopLevelVariableImpl<Any?> =
        KTopLevelVariableImpl<Any?>(name, owner)

fun mutableTopLevelVariable(name: String, owner: KPackageImpl): KMutableTopLevelVariableImpl<Any?> =
        KMutableTopLevelVariableImpl<Any?>(name, owner)

fun <T> topLevelExtensionProperty(name: String, owner: KPackageImpl, receiver: Class<T>): KTopLevelExtensionPropertyImpl<T, Any?> =
        KTopLevelExtensionPropertyImpl<T, Any?>(name, owner, receiver)

fun <T> mutableTopLevelExtensionProperty(name: String, owner: KPackageImpl, receiver: Class<T>): KMutableTopLevelExtensionPropertyImpl<T, Any?> =
        KMutableTopLevelExtensionPropertyImpl<T, Any?>(name, owner, receiver)
