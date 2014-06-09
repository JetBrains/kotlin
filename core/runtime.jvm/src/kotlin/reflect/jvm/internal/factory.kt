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


fun <T> kClass(jClass: Class<T>): KClassImpl<T> =
        KClassImpl<T>(jClass)

fun kPackage(jClass: Class<*>): KPackageImpl =
        KPackageImpl(jClass)

fun topLevelProperty(name: String, owner: KPackageImpl): KTopLevelPropertyImpl<Any?> =
        KTopLevelPropertyImpl<Any?>(name, owner)

fun mutableTopLevelProperty(name: String, owner: KPackageImpl): KMutableTopLevelPropertyImpl<Any?> =
        KMutableTopLevelPropertyImpl<Any?>(name, owner)

fun <T> extensionProperty(name: String, owner: KPackageImpl, receiver: Class<T>): KExtensionPropertyImpl<T, Any?> =
        KExtensionPropertyImpl<T, Any?>(name, owner, receiver)

fun <T> mutableExtensionProperty(name: String, owner: KPackageImpl, receiver: Class<T>): KMutableExtensionPropertyImpl<T, Any?> =
        KMutableExtensionPropertyImpl<T, Any?>(name, owner, receiver)

fun <T : Any> memberProperty(name: String, owner: KClassImpl<T>): KMemberPropertyImpl<T, Any?> =
        KMemberPropertyImpl<T, Any?>(name, owner)

fun <T : Any> mutableMemberProperty(name: String, owner: KClassImpl<T>): KMutableMemberPropertyImpl<T, Any?> =
        KMutableMemberPropertyImpl<T, Any?>(name, owner)

